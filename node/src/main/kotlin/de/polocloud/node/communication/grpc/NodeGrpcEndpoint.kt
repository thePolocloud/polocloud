package de.polocloud.node.communication.grpc

import de.polocloud.common.Address
import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.communication.GrpcEndpoint
import de.polocloud.common.communication.tls.MtlsConfig
import de.polocloud.node.cluster.election.NodeElectionService
import de.polocloud.node.communication.cli.session.CliSessionCleanup
import de.polocloud.node.communication.cli.session.ICliSessionManager
import de.polocloud.node.communication.impl.cluster.ClusterServiceImpl
import de.polocloud.node.communication.impl.node.NodeServiceImpl
import de.polocloud.node.communication.impl.services.ServiceApiServiceImpl
import de.polocloud.node.communication.impl.services.ServiceManagerImpl
import de.polocloud.node.communication.impl.services.ServiceRegistrationServiceImpl
import de.polocloud.node.communication.interceptor.CliSessionInterceptor
import de.polocloud.node.communication.interceptor.IpWhitelistInterceptor
import de.polocloud.node.communication.registration.cli.CliRegistrationService
import de.polocloud.node.group.GroupService
import de.polocloud.node.security.NodeCertificateStorage
import de.polocloud.node.services.ServiceProvider

/**
 * gRPC endpoint for the cluster node.
 *
 * Hosts all services on a single mTLS port ([de.polocloud.common.communication.tls.ClientAuthMode.REQUIRE]):
 * - CLI registration and commands  → trusted via the shared CA, additionally IP-whitelisted
 * - Node-to-node communication     → trusted via the same CA
 *
 * Both client types share one CA, so a single [MtlsConfig.mutual] call is sufficient.
 * If separate CAs are introduced later, switch to [MtlsConfig.mutual] with multiple files.
 *
 * Session cleanup is owned here because this class controls the server lifecycle —
 * cleanup starts when the server starts and stops when the server stops.
 */
class NodeGrpcEndpoint(
    address: Address,
    cliRegistrationService: CliRegistrationService,
    cliSessionManager: ICliSessionManager,
    groupService: GroupService,
    serviceProvider: ServiceProvider,
    electionService: NodeElectionService,
) : Closeable {

    private val executor = GrpcModule.createExecutor(groupService, serviceProvider)

    private val nodeService = NodeServiceImpl(executor, serviceProvider, electionService)
    private val clusterService = ClusterServiceImpl(executor)
    private val serviceManager = ServiceManagerImpl(executor, serviceProvider)
    // Also exposed here (not only on ServiceGrpcEndpoint) so a peer node can fetch this
    // node's local ServiceData when assembling the cluster-wide FindServices view.
    private val serviceApiService = ServiceApiServiceImpl(executor)
    // Lets a non-head node forward a locally-launched service's CSR to whichever node
    // currently holds the cluster CA's private key — see ServiceRegistrationServiceImpl.
    private val serviceRegistrationService = ServiceRegistrationServiceImpl()

    private val sessionCleanup = CliSessionCleanup(cliSessionManager)

    private val server = GrpcEndpoint.Builder(address)
        .tls(
            MtlsConfig.mutual(
                cert = NodeCertificateStorage.certificateFile(),
                key = NodeCertificateStorage.privateKeyFile(),
                caCert = NodeCertificateStorage.caCertificateFile(),
            )
        )
        .interceptedService(
            cliRegistrationService,
            IpWhitelistInterceptor(),
            CliSessionInterceptor(cliSessionManager)
        )
        .interceptedService(
            nodeService,
            IpWhitelistInterceptor(),
            CliSessionInterceptor(cliSessionManager)
        )
        .interceptedService(
            clusterService,
            IpWhitelistInterceptor(),
            CliSessionInterceptor(cliSessionManager)
        )
        .interceptedService(
            serviceManager,
            IpWhitelistInterceptor(),
            CliSessionInterceptor(cliSessionManager)
        )
        .interceptedService(
            serviceApiService,
            IpWhitelistInterceptor(),
            CliSessionInterceptor(cliSessionManager)
        )
        .interceptedService(
            serviceRegistrationService,
            IpWhitelistInterceptor(),
            CliSessionInterceptor(cliSessionManager)
        )
        .build()

    fun start() {
        server.start()
        sessionCleanup.start()
    }

    override fun close(mode: ShutdownMode) {
        nodeService.broadcastShutdown()
        sessionCleanup.close(mode)
        server.close(mode)
    }
}
