package de.polocloud.node.communication.handler.services

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.services.ServiceProvider
import de.polocloud.proto.GetServiceResourceUsageRequest
import de.polocloud.proto.GetServiceResourceUsageResponse

/**
 * Handles a peer's `GetServiceResourceUsage` request — the cross-node counterpart of the
 * CLI's `service <name>` info view, used when the service is running on a different node
 * than the one the CLI command was issued on. See [de.polocloud.node.terminal.impl.ServiceCommand].
 */
class GetServiceResourceUsageServerHandler(
    private val serviceProvider: ServiceProvider,
) : GrpcServerHandler<GetServiceResourceUsageRequest, GetServiceResourceUsageResponse> {

    override suspend fun handle(request: GetServiceResourceUsageRequest, context: GrpcServerContext): GetServiceResourceUsageResponse {
        val local = serviceProvider.findLocal(request.serviceName)
            ?: return GetServiceResourceUsageResponse.newBuilder()
                .setFound(false)
                .setMessage("Service '${request.serviceName}' is not running on this node.")
                .build()

        return GetServiceResourceUsageResponse.newBuilder()
            .setFound(true)
            .setCpuUsage(local.cpuUsage)
            .setUsedMemory(local.usedMemory)
            .setPid(local.process?.pid() ?: -1L)
            .build()
    }
}