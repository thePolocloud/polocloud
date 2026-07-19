package de.polocloud.node.communication.handler.group

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.group.GroupService
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.cluster.ClusterGroupShutdown
import de.polocloud.proto.DeleteGroupRequest
import de.polocloud.proto.DeleteGroupResponse

class DeleteGroupServerHandler(
    private val groupService: GroupService,
    private val serviceProvider: ServiceProvider,
) : GrpcServerHandler<DeleteGroupRequest, DeleteGroupResponse> {

    override suspend fun handle(
        request: DeleteGroupRequest,
        context: GrpcServerContext
    ): DeleteGroupResponse {
        val group = groupService.find(request.name)
            ?: return DeleteGroupResponse.newBuilder().setSuccess(false).build()

        // Stop the group's running/queued services across the whole cluster before
        // removing it — see ClusterGroupShutdown.
        ClusterGroupShutdown.shutdownAcrossCluster(group.name, serviceProvider)
        groupService.delete(group)

        return DeleteGroupResponse.newBuilder().setSuccess(true).build()
    }
}
