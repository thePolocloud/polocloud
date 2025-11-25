package dev.httpmarco.polocloud.agent.player

import com.google.protobuf.Any
import com.google.protobuf.Empty
import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.v1.player.PlayerActorAuthRequest
import dev.httpmarco.polocloud.v1.player.PlayerActorRegister
import dev.httpmarco.polocloud.v1.player.PlayerControllerGrpc
import dev.httpmarco.polocloud.v1.player.PlayerCountResponse
import dev.httpmarco.polocloud.v1.player.PlayerFindByNameRequest
import dev.httpmarco.polocloud.v1.player.PlayerFindByServiceRequest
import dev.httpmarco.polocloud.v1.player.PlayerFindResponse
import dev.httpmarco.polocloud.v1.proto.EventProviderOuterClass
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver

class PlayerGrpcService : PlayerControllerGrpc.PlayerControllerImplBase() {

    override fun findAll(request: Empty, responseObserver: StreamObserver<PlayerFindResponse>) {
        val builder = PlayerFindResponse.newBuilder()
        val playerStorage = Agent.playerStorage

        for (player in playerStorage.findAll()) {
            builder.addPlayers(player.toSnapshot())
        }

        responseObserver.onNext(builder.build())
        responseObserver.onCompleted()
    }

    override fun findByName(request: PlayerFindByNameRequest, responseObserver: StreamObserver<PlayerFindResponse>) {
        val builder = PlayerFindResponse.newBuilder()
        val playerStorage = Agent.playerStorage

        val playerToReturn = if (request.name.isNotEmpty()) {
            playerStorage.findByName(request.name)?.let { listOf(it) } ?: emptyList()
        } else {
            playerStorage.findAll()
        }

        for (player in playerToReturn) {
            builder.addPlayers(player.toSnapshot())
        }

        responseObserver.onNext(builder.build())
        responseObserver.onCompleted()
    }

    override fun findByService(
        request: PlayerFindByServiceRequest,
        responseObserver: StreamObserver<PlayerFindResponse>
    ) {
        val builder = PlayerFindResponse.newBuilder()
        val playerStorage = Agent.playerStorage

        val playerToReturn = if (request.currentServiceName.isNotEmpty()) {
            playerStorage.findByService(request.currentServiceName)
        } else {
            playerStorage.findAll()
        }

        for (player in playerToReturn) {
            builder.addPlayers(player.toSnapshot())
        }

        responseObserver.onNext(builder.build())
        responseObserver.onCompleted()
    }

    override fun playerCount(request: Empty, responseObserver: StreamObserver<PlayerCountResponse>) {
        val count = Agent.playerStorage.playerCount()
        val response = PlayerCountResponse.newBuilder()
            .setCount(count)
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun registerActor(request: PlayerActorAuthRequest, responseObserver: StreamObserver<PlayerActorRegister?>?) {
        val service = Agent.serviceProvider().find(request.serviceId)
        val observer = responseObserver as ServerCallStreamObserver<*>

        observer.setOnCancelHandler {
            //TODO
            //Agent.eventService.detach(request.eventName, request.serviceName)
        }

    }
}