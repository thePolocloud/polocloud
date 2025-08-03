package dev.httpmarco.polocloud.agent.groups

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.utils.asStringMap
import dev.httpmarco.polocloud.v1.proto.GroupControllerGrpc
import dev.httpmarco.polocloud.v1.proto.GroupProvider
import io.grpc.stub.StreamObserver

class GroupGrpcService : GroupControllerGrpc.GroupControllerImplBase() {

    override fun find(
        request: GroupProvider.FindGroupRequest,
        responseObserver: StreamObserver<GroupProvider.FindGroupResponse>
    ) {

        val builder = GroupProvider.FindGroupResponse.newBuilder()
        val groupStorage = Agent.runtime.groupStorage()

        val groupsToReturn = if (request.name.isNotEmpty()) {
            groupStorage.item(request.name)?.let { listOf(it) } ?: emptyList()
        } else {
            groupStorage.items()
        }

        for (group in groupsToReturn) {
            builder.addGroups(
                GroupProvider.GroupSnapshot.newBuilder()
                    .setName(group.data.name)
                    .putAllProperties(group.data.properties.asStringMap())
                    .build()
            )
        }

        responseObserver.onNext(builder.build())
        responseObserver.onCompleted()

    }
}