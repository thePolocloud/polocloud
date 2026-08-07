package de.polocloud.node.communication.handler.group

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.communication.handler.CallerAuthorization
import de.polocloud.node.group.GroupProtoMapper
import de.polocloud.node.group.GroupService
import de.polocloud.proto.CreateGroupRequest
import de.polocloud.proto.GroupData

class CreateGroupServerHandler(private val groupService: GroupService) : GrpcServerHandler<CreateGroupRequest, GroupData> {

    override suspend fun handle(
        request: CreateGroupRequest,
        context: GrpcServerContext
    ): GroupData {
        CallerAuthorization.requireTrustedCaller(context, "create groups")

        val group = GroupProtoMapper.toDomain(request.group)

        check(group.name.isNotBlank()) { "Group name must not be blank" }
        check(!groupService.exists(group.name)) { "A group with the name '${group.name}' already exists" }

        return GroupProtoMapper.toProto(groupService.create(group))
    }
}
