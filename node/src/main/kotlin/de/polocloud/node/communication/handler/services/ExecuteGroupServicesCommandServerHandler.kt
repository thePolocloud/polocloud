package de.polocloud.node.communication.handler.services

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.services.ServiceProvider
import de.polocloud.proto.ExecuteGroupServicesCommandRequest
import de.polocloud.proto.ExecuteGroupServicesCommandResponse

/**
 * Handles a peer's `ExecuteGroupServicesCommand` request — the cross-node counterpart
 * used by `group <name> execute <command>`, so this node's own replicas of the group get
 * the command too. See [de.polocloud.node.services.cluster.ClusterGroupExecute].
 */
class ExecuteGroupServicesCommandServerHandler(
    private val serviceProvider: ServiceProvider,
) : GrpcServerHandler<ExecuteGroupServicesCommandRequest, ExecuteGroupServicesCommandResponse> {

    override suspend fun handle(request: ExecuteGroupServicesCommandRequest, context: GrpcServerContext): ExecuteGroupServicesCommandResponse {
        val executed = serviceProvider.executeGroupCommand(request.groupName, request.command)
        return ExecuteGroupServicesCommandResponse.newBuilder().setExecutedCount(executed).build()
    }
}
