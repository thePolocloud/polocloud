package de.polocloud.node.core.context

import de.polocloud.common.ShutdownMode
import de.polocloud.common.configuration.ConfigurationHolder
import de.polocloud.node.cluster.election.NodeElectionService
import de.polocloud.node.cluster.node.LocalNodeContainer
import de.polocloud.node.communication.grpc.NodeGrpcEndpoint
import de.polocloud.node.communication.grpc.ServiceGrpcEndpoint
import de.polocloud.node.communication.registration.node.RegistrationManager
import de.polocloud.node.core.configuration.NodeConfigurations
import de.polocloud.node.group.GroupService
import de.polocloud.node.identity.NodeIdentityService
import de.polocloud.node.module.ModuleManager
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.terminal.CliTerminal

class NodeRuntimeContext(
    var holder: ConfigurationHolder<NodeConfigurations>,
    val localNodeContainer: LocalNodeContainer,
    val registrationManager: RegistrationManager,
    grpcEndpoint: NodeGrpcEndpoint,
    serviceGrpcEndpoint: ServiceGrpcEndpoint,
    val groupService: GroupService,
    val serviceProvider: ServiceProvider,
    val electionService: NodeElectionService,
    val identityService: NodeIdentityService,
) {

    var grpcEndpoint: NodeGrpcEndpoint = grpcEndpoint
        private set

    var serviceGrpcEndpoint: ServiceGrpcEndpoint = serviceGrpcEndpoint
        private set

    // Declared before `cli`: CliTerminal's constructor registers ModuleCommand, which
    // needs this already initialized.
    val moduleManager: ModuleManager = ModuleManager(this, electionService)

    val cli: CliTerminal = CliTerminal(this)

    /**
     * Swaps both mTLS-secured gRPC endpoints for freshly built ones — used after a live
     * `cluster join` picks up cluster-issued certificates that the already-running
     * [grpcEndpoint]/[serviceGrpcEndpoint] can't adopt in place (their TLS material is
     * baked into the underlying Netty `Server` once, at construction). Synchronized so a
     * concurrent read of [grpcEndpoint]/[serviceGrpcEndpoint] never observes a half-swapped
     * state, and so this can't itself run twice concurrently.
     *
     * Closes the old endpoints only once the new ones are already built and about to bind
     * — there's nothing to gain from closing earlier, only a longer window where this node
     * is unreachable. A short retry covers the OS not having released the old port yet.
     */
    @Synchronized
    fun replaceGrpcEndpoints(
        buildGrpc: () -> NodeGrpcEndpoint,
        buildServiceGrpc: () -> ServiceGrpcEndpoint,
    ) {
        val oldGrpc = grpcEndpoint
        val oldServiceGrpc = serviceGrpcEndpoint

        oldGrpc.close(ShutdownMode.GRACEFUL)
        oldServiceGrpc.close(ShutdownMode.GRACEFUL)

        grpcEndpoint = retryBind { buildGrpc().also { it.start() } }
        serviceGrpcEndpoint = retryBind { buildServiceGrpc().also { it.start() } }
    }

    private fun <T> retryBind(attempts: Int = 3, delayMillis: Long = 200, build: () -> T): T {
        var lastError: Exception? = null
        repeat(attempts) {
            try {
                return build()
            } catch (ex: Exception) {
                lastError = ex
                Thread.sleep(delayMillis)
            }
        }
        throw IllegalStateException("Could not rebind the node's gRPC endpoint after $attempts attempts", lastError)
    }

}