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
import de.polocloud.node.communication.registration.cli.CliRegistrationService
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
import kotlin.time.Clock.System.now

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

            return NodeRuntimeContext(holder, container, registrationManager, grpc, serviceGrpc, null, groupService, serviceProvider)
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

            return NodeRuntimeContext(holder, container, registrationManager, grpc, serviceGrpc, null, groupService, serviceProvider)
        }

        if (launchProperties.clusterRegistration == null) {
            logger.trInfo("cluster", "cluster.validation.failed")
            throw IllegalStateException("This node '$localId' is not registered in the cluster and no registration token was provided.")
        }


        // Confirmed by actually running this against a rejected join (reused token):
        // without this check, the code fell through to grpc.start() and eventually
        // `NodeRepository.find(localId)!!` below, crashing on an unrelated NPE instead
        // of the clear, actionable error a denied join should produce.
        if (!registrationManager.tryJoinCluster(launchProperties.clusterRegistration, localId, launchProperties.group)) {
            throw IllegalStateException(
                "This node '$localId' was denied registration by the cluster — check that the registration token is valid, unused, and this node's version matches the cluster's."
            )
        }
        adoptClusterCaKeyPair()
        adoptForwardingSecret(serviceProvider)

        grpc.start()
        serviceGrpc.start()

        val headConnection = NodeGrpcClient()
        headConnection.connect(launchProperties.clusterRegistration.address)

        val nodeData = NodeRepository.find(localId)
        container = LocalNodeContainer(nodeData!!)

        return NodeRuntimeContext(holder, container, registrationManager, grpc, serviceGrpc, headConnection, groupService, serviceProvider)
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