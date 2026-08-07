package de.polocloud.node.communication.handler.group

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.communication.handler.CallerAuthorization
import de.polocloud.node.group.GroupProtoMapper
import de.polocloud.node.group.GroupService
import de.polocloud.proto.GroupData
import de.polocloud.proto.UpdateGroupRequest

class UpdateGroupServerHandler(private val groupService: GroupService) : GrpcServerHandler<UpdateGroupRequest, GroupData> {

    override suspend fun handle(
        request: UpdateGroupRequest,
        context: GrpcServerContext
    ): GroupData {
        CallerAuthorization.requireTrustedCaller(context, "update groups")

        val group = GroupProtoMapper.toDomain(request.group)

        check(group.name.isNotBlank()) { "Group name must not be blank" }
        check(groupService.exists(group.name)) { "No group with the name '${group.name}' exists" }

        // GroupService.update publishes the GroupUpdatedEvent for every update path.
        return GroupProtoMapper.toProto(groupService.update(group))
    }
}
