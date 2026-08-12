package de.polocloud.node.communication.grpc

import de.polocloud.common.Address
import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.communication.GrpcEndpoint
import de.polocloud.common.communication.tls.MtlsConfig
import de.polocloud.node.communication.impl.event.EventProviderServiceImpl
import de.polocloud.node.communication.impl.group.GroupApiServiceImpl
import de.polocloud.node.communication.impl.player.PlayerApiServiceImpl
import de.polocloud.node.communication.impl.services.ServiceApiServiceImpl
import de.polocloud.node.communication.interceptor.ServiceIdentityInterceptor
import de.polocloud.node.group.GroupService
import de.polocloud.node.security.NodeCertificateStorage
import de.polocloud.node.services.ServiceProvider

/**
 * Dedicated mTLS gRPC endpoint for **services and plugins** that use the
 * standalone Polocloud API.
 *
 * Intentionally kept separate from [NodeGrpcEndpoint] — which serves CLI and
 * node-to-node traffic — so plugin API calls never share a port or the
 * CLI-specific interceptor chain (IP whitelist, CLI sessions) with the cluster
 * control plane. It does carry its own, much lighter [ServiceIdentityInterceptor]
 * though, so the calling service's identity is still available for logging
 * (see that class's doc for why this endpoint needs its own copy).
 *
 * Trust is anchored on the same node CA, so the per-service identity the node
 * provisions when it launches a service (certificate + CA via
 * [de.polocloud.node.security.ServiceIdentityProvisioner]) is accepted here.
 */
class ServiceGrpcEndpoint(
    address: Address,
    groupService: GroupService,
    serviceProvider: ServiceProvider,
) : Closeable {

    private val executor = GrpcModule.createExecutor(groupService, serviceProvider)
    private val groupApiService = GroupApiServiceImpl(executor)
    private val serviceApiService = ServiceApiServiceImpl(executor, serviceProvider)
    private val playerApiService = PlayerApiServiceImpl(executor)
    private val eventProviderService = EventProviderServiceImpl()

    private val server = GrpcEndpoint.Builder(address)
        .tls(
            MtlsConfig.mutual(
                cert = NodeCertificateStorage.certificateFile(),
                key = NodeCertificateStorage.privateKeyFile(),
                caCert = NodeCertificateStorage.caCertificateFile(),
            )
        )
        .interceptedService(groupApiService, ServiceIdentityInterceptor())
        .interceptedService(serviceApiService, ServiceIdentityInterceptor())
        .interceptedService(playerApiService, ServiceIdentityInterceptor())
        .interceptedService(eventProviderService, ServiceIdentityInterceptor())
        .build()

    fun start() = server.start()

    override fun close(mode: ShutdownMode) = server.close(mode)
}
