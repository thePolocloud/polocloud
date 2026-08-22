package de.polocloud.node.communication.handler.services

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.services.ServiceProvider
import de.polocloud.proto.RestartGroupServicesRequest
import de.polocloud.proto.RestartGroupServicesResponse

/**
 * Handles a peer's `RestartGroupServices` request — the cross-node counterpart used by
 * `group <name> restart`, so this node's own replicas of the group get restarted too.
 * See [de.polocloud.node.services.cluster.ClusterGroupRestart].
 */
class RestartGroupServicesServerHandler(
    private val serviceProvider: ServiceProvider,
) : GrpcServerHandler<RestartGroupServicesRequest, RestartGroupServicesResponse> {

    override suspend fun handle(request: RestartGroupServicesRequest, context: GrpcServerContext): RestartGroupServicesResponse {
        val restarted = serviceProvider.restartGroup(request.groupName)
        return RestartGroupServicesResponse.newBuilder().setRestartedCount(restarted).build()
    }
}
