package dev.httpmarco.polocloud.agent.groups

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.v1.proto.GroupProvider
import dev.httpmarco.polocloud.v1.proto.GroupProviderHandlerGrpc
import io.grpc.stub.StreamObserver

class GroupGrpcService : GroupProviderHandlerGrpc.GroupProviderHandlerImplBase() {

    override fun find(request: GroupProvider.FindRequest, responseObserver: StreamObserver<GroupProvider.FindResponse>) {

        var group = Agent.instance.runtime.groupStorage().item(request.name)



        TODO()
    }
}