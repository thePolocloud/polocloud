package de.polocloud.node.identity

import de.polocloud.common.Address
import de.polocloud.common.configuration.ConfigurationHolder
import de.polocloud.i18n.api.trInfo
import de.polocloud.node.bootstrap.properties.NodeProperties
import de.polocloud.node.cluster.election.NodeElectionService
import de.polocloud.node.cluster.node.LocalNodeContainer
import de.polocloud.node.cluster.node.NodeFactory
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.communication.cli.session.ICliSessionManager
import de.polocloud.node.communication.grpc.NodeGrpcClient
import de.polocloud.node.communication.grpc.NodeGrpcEndpoint
import de.polocloud.node.communication.grpc.ServiceGrpcEndpoint
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.communication.registration.cli.CliRegistrationService
import de.polocloud.node.communication.registration.node.RegistrationInfo
import de.polocloud.node.communication.registration.node.RegistrationManager
import de.polocloud.node.core.configuration.NodeConfigurations
import de.polocloud.node.core.context.NodeRuntimeContext
import de.polocloud.node.group.GroupService
import de.polocloud.node.identity.provider.NodeIdProvider
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.security.NodeCertificateStorage
import de.polocloud.proto.FetchClusterCaRequest
import de.polocloud.proto.FetchForwardingSecretRequest
import de.polocloud.proto.NodeServiceGrpcKt
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.time.Clock.System.now

/** Outcome of a join handshake against an existing cluster — see [NodeIdentityService.joinLive]. */
sealed interface JoinResult {
    data class Success(val nodeData: NodeData) : JoinResult
    data class Denied(val reason: String) : JoinResult
    data class RecordMissing(val reason: String) : JoinResult
    /** The target couldn't be reached at all (network/transport failure) — distinct from [Denied], which means it was reached but rejected the token. */
    data class Unreachable(val reason: String) : JoinResult
}

class NodeIdentityService(
    private val nodeId: NodeIdProvider,
    private val holder: ConfigurationHolder<NodeConfigurations>,
    private val registrationManager: RegistrationManager,
    private val cliRegistrationService: CliRegistrationService,
    private val cliSessionManager: ICliSessionManager,
    private val electionService: NodeElectionService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    lateinit var container: LocalNodeContainer

    fun resolve(launchProperties: NodeProperties): NodeRuntimeContext {
        val localId = nodeId.get()

        NodeCertificateStorage.nodeId = localId.toString()
        NodeCertificateStorage.configuredHostname = holder.value.general.hostname
        NodeCertificateStorage.initialize()

        val bindAddress = resolveBindAddress(launchProperties)

        val serviceProvider = ServiceProvider(
            nodePort = holder.value.general.apiAddress.port,
            // Host services are advertised under. Loopback by default (single-host);
            // set general.serviceHostname to the node's reachable address for a cluster.
            nodeHost = holder.value.general.serviceHostname,
            nodeId = localId.toString(),
        )
        // Shares serviceProvider's PlatformService so a group's default templates are
        // resolved (proxy vs. server) against the same loaded platform set services start
        // from, rather than a second, independently-loaded copy.
        val groupService = GroupService(serviceProvider.platformService)

        val grpc = NodeGrpcEndpoint(
            bindAddress,
            cliRegistrationService,
            cliSessionManager,
            groupService,
            serviceProvider,
            electionService
        )

        val serviceGrpc = ServiceGrpcEndpoint(
            Address(holder.value.general.apiAddress.hostname, holder.value.general.apiAddress.port),
            groupService,
            serviceProvider
        )

        if (NodeRepository.count() == 0L) {
            logger.trInfo("cluster", "cluster.node.identity.created")

            container = LocalNodeContainer(NodeFactory.createInitial(Address(holder.value.general.hostname, holder.value.general.bindAddress.port), launchProperties.group))
            NodeRepository.save(container.data)

            val clusterToken = registrationManager.tokenManger.createInitialToken()
            val expire = Instant.ofEpochMilli(clusterToken.expiresAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()

            logger.trInfo(
                "cluster",
                "cluster.node.identity.alert.token",
                "clusterToken" to clusterToken.token,
                "expire" to expire
            )

            grpc.start()
            serviceGrpc.start()

            return NodeRuntimeContext(holder, container, registrationManager, grpc, serviceGrpc, groupService, serviceProvider, electionService, this)
        }

        val possibleNode = NodeRepository.find(localId)
        if (possibleNode != null) {
            logger.trInfo("cluster", "cluster.node.identity.detected", "nodeId" to localId.toString())

            container = LocalNodeContainer(possibleNode)
            container.markStarting()

            container.data.lastConnection = now()
            NodeRepository.save(container.data)

            grpc.start()
            serviceGrpc.start()

            return NodeRuntimeContext(holder, container, registrationManager, grpc, serviceGrpc, groupService, serviceProvider, electionService, this)
        }

        if (launchProperties.clusterRegistration == null) {
            logger.trInfo("cluster", "cluster.validation.failed")
            throw IllegalStateException("This node '$localId' is not registered in the cluster and no registration token was provided.")
        }

        // Confirmed by actually running this against a rejected join (reused token):
        // without the handshake's own checks, boot fell through to grpc.start() and
        // eventually `NodeRepository.find(localId)!!`, crashing on an unrelated NPE
        // instead of the clear, actionable error a denied join should produce.
        when (val result = performJoinHandshake(launchProperties.clusterRegistration, localId, launchProperties.group, serviceProvider)) {
            is JoinResult.Denied -> throw IllegalStateException(result.reason)
            is JoinResult.RecordMissing -> throw IllegalStateException(result.reason)
            is JoinResult.Unreachable -> throw IllegalStateException(result.reason)
            is JoinResult.Success -> container = LocalNodeContainer(result.nodeData)
        }

        grpc.start()
        serviceGrpc.start()

        return NodeRuntimeContext(holder, container, registrationManager, grpc, serviceGrpc, groupService, serviceProvider, electionService, this)
    }

    /**
     * Performs the join handshake against an existing cluster — CSR/registration, then
     * best-effort CA key pair and forwarding secret adoption — and re-reads the resulting
     * [NodeData]. Shared by the boot path ([resolve]) and the live, in-process path
     * ([joinLive]) so both go through the exact same handshake logic.
     */
    private fun performJoinHandshake(
        registrationInfo: RegistrationInfo,
        localId: UUID,
        group: String,
        serviceProvider: ServiceProvider,
    ): JoinResult {
        // RegistrationClient.tryRegister deliberately throws rather than returning a
        // rejection for transport-level failures (unreachable host, connection refused,
        // timeout, ...) — a genuinely different failure mode than the cluster reaching and
        // rejecting the token, so it's surfaced as its own JoinResult here instead of
        // leaking a raw exception into resolve()/joinLive's callers. The interactive
        // `cluster join` wizard already pre-checks reachability to make this rare in
        // practice, but a target that goes away in the brief window after that check (or a
        // boot-time -Dpolocloud.join.host that was never reachable) still needs a clean,
        // actionable result rather than an uncaught crash.
        val accepted = try {
            registrationManager.tryJoinCluster(registrationInfo, localId, group)
        } catch (exception: Exception) {
            logger.warn("Could not reach the cluster at '{}': {}", registrationInfo.address, exception.message)
            return JoinResult.Unreachable(
                "Could not reach the cluster at '${registrationInfo.address}': ${exception.message ?: exception.javaClass.simpleName}"
            )
        }
        if (!accepted) {
            return JoinResult.Denied(
                "This node '$localId' was denied registration by the cluster — check that the registration token is valid, unused, and this node's version matches the cluster's."
            )
        }
        adoptClusterCaKeyPair()
        adoptForwardingSecret(serviceProvider)

        // Mirrors tryJoinCluster's own check: the join just succeeded and this node was in
        // NodeRepository as of that call returning true, but nothing prevents it from being
        // evicted (e.g. pruned as unreachable, or removed by an operator) in the brief
        // window between that success and this read.
        val nodeData = NodeRepository.find(localId)
            ?: return JoinResult.RecordMissing(
                "This node '$localId' was registered by the cluster but its record disappeared before startup could finish — it may have been evicted concurrently; retry the join."
            )
        return JoinResult.Success(nodeData)
    }

    /**
     * Live, in-process counterpart to the join branch of [resolve] — used by the
     * interactive `cluster join` terminal command so a fresh, standalone node can join an
     * existing cluster without restarting the node's JVM process. Performs the same
     * handshake as boot-time joining, then rebuilds this node's gRPC endpoints in place
     * (their mTLS material is baked in once at construction, so picking up the
     * cluster-issued certificate requires a fresh [de.polocloud.node.communication.grpc.NodeGrpcEndpoint]/
     * [de.polocloud.node.communication.grpc.ServiceGrpcEndpoint], not a process restart) —
     * see [NodeRuntimeContext.replaceGrpcEndpoints].
     */
    fun joinLive(registrationInfo: RegistrationInfo, group: String, context: NodeRuntimeContext): JoinResult {
        val result = performJoinHandshake(registrationInfo, nodeId.get(), group, context.serviceProvider)
        if (result is JoinResult.Success) {
            context.localNodeContainer.adoptJoinedIdentity(result.nodeData)

            val bindAddress = Address(holder.value.general.bindAddress.hostname, holder.value.general.bindAddress.port)
            context.replaceGrpcEndpoints(
                buildGrpc = {
                    NodeGrpcEndpoint(
                        bindAddress,
                        cliRegistrationService,
                        cliSessionManager,
                        context.groupService,
                        context.serviceProvider,
                        context.electionService,
                    )
                },
                buildServiceGrpc = {
                    ServiceGrpcEndpoint(
                        Address(holder.value.general.apiAddress.hostname, holder.value.general.apiAddress.port),
                        context.groupService,
                        context.serviceProvider,
                    )
                },
            )
        }
        return result
    }

    /**
     * Adopts the real cluster CA key pair from the current head right after this node
     * joins, over the already-mTLS-secured [NodeGrpcClient] channel (the join handshake
     * itself only transports the CA *certificate*, never the key — see
     * [de.polocloud.node.security.NodeCertificateStorage.adoptClusterCaKeyPair]).
     *
     * Best-effort: a failure here only means this node can't yet safely be promoted to
     * head by leader election — it can still serve as a regular cluster member and will
     * retry on its next restart, so a transient failure here must not abort the join.
     */
    private fun adoptClusterCaKeyPair() {
        val head = NodeRepository.findAll().firstOrNull { it.head }
        if (head == null) {
            logger.warn("Could not adopt the cluster CA key pair — no head node is currently known")
            return
        }

        val client = NodeGrpcClient()
        runCatching {
            client.connect(Address(head.hostname, head.port))
            val stub = NodeServiceGrpcKt.NodeServiceCoroutineStub(client.channel())
            val response = runBlocking { stub.fetchClusterCa(FetchClusterCaRequest.getDefaultInstance()) }
            if (!response.available) {
                error(response.message)
            }
            NodeCertificateStorage.adoptClusterCaKeyPair(response.caPrivateKey, response.caPublicKey)
        }.onFailure { ex ->
            logger.warn("Could not adopt the cluster CA key pair from head '{}' — this node cannot safely become head until a later restart succeeds: {}", head.name(), ex.message)
        }
        client.disconnect()
    }

    /**
     * Adopts the cluster's real forwarding secret from the current head right after this
     * node joins, over the already-mTLS-secured [NodeGrpcClient] channel — otherwise this
     * node would keep the random secret [de.polocloud.node.forwarding.ForwardingHandler]
     * generated for itself on first start, and the proxy/services it launches would never
     * agree with the rest of the cluster on the shared forwarding token.
     *
     * Best-effort like [adoptClusterCaKeyPair]: a failure here is logged and retried on
     * the next restart rather than aborting the join, since the node can still function —
     * just without working forwarding to/from services on other nodes until it succeeds.
     */
    private fun adoptForwardingSecret(serviceProvider: ServiceProvider) {
        val head = NodeRepository.findAll().firstOrNull { it.head }
        if (head == null) {
            logger.warn("Could not adopt the cluster forwarding secret — no head node is currently known")
            return
        }

        val client = NodeGrpcClient()
        runCatching {
            client.connect(Address(head.hostname, head.port))
            val stub = NodeServiceGrpcKt.NodeServiceCoroutineStub(client.channel())
            val response = runBlocking { stub.fetchForwardingSecret(FetchForwardingSecretRequest.getDefaultInstance()) }
            if (!response.available) {
                error(response.message)
            }
            serviceProvider.forwardingHandler.adopt(response.secret)
        }.onFailure { ex ->
            logger.warn("Could not adopt the cluster forwarding secret from head '{}' — services on this node may not be able to forward to/from peers until a later restart succeeds: {}", head.name(), ex.message)
        }
        client.disconnect()
    }

    /**
     * Resolves the effective bind address of this node.
     *
     * <p>Resolution priority:</p>
     * <ol>
     *     <li>Address provided via {@link NodeLaunchConfig}</li>
     *     <li>Bind address from persisted configuration</li>
     * </ol>
     *
     * @return the resolved {@link Address} used for the gRPC endpoint
     */
    private fun resolveBindAddress(launchProperties: NodeProperties): Address {
        val launchAddress = launchProperties.address
        val defaultAddress = Address(holder.value.general.bindAddress.hostname, holder.value.general.bindAddress.port)

        if (launchAddress != null) {
            val hostname = launchAddress.hostname.takeIf { it.isNotBlank() } ?: defaultAddress.hostname
            val port = launchAddress.port.takeIf { it > 0 } ?: defaultAddress.port

            require(port in 1..65535) { "Port must be between 1 and 65535 but was $port" }

            return Address(hostname, port)
        }
        return defaultAddress
    }
}