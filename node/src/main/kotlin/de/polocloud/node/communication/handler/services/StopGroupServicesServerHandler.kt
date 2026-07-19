package de.polocloud.node.communication.handler.services

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.services.ServiceProvider
import de.polocloud.proto.StopGroupServicesRequest
import de.polocloud.proto.StopGroupServicesResponse

/**
 * Handles a peer's `StopGroupServices` request — the cross-node counterpart used when a
 * group is deleted, so this node's own replicas of it are stopped too. See
 * [de.polocloud.node.services.cluster.ClusterGroupShutdown].
 */
class StopGroupServicesServerHandler(
    private val serviceProvider: ServiceProvider,
) : GrpcServerHandler<StopGroupServicesRequest, StopGroupServicesResponse> {

    override suspend fun handle(request: StopGroupServicesRequest, context: GrpcServerContext): StopGroupServicesResponse {
        val stoppedCount = serviceProvider.localServices.count { it.groupName.equals(request.groupName, ignoreCase = true) }
        serviceProvider.shutdownGroup(request.groupName)
        return StopGroupServicesResponse.newBuilder().setStoppedCount(stoppedCount).build()
    }
}