package dev.httpmarco.polocloud.agent.player

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.v1.player.PlayerControllerGrpc
import dev.httpmarco.polocloud.v1.player.PlayerCountRequest
import dev.httpmarco.polocloud.v1.player.PlayerCountResponse
import dev.httpmarco.polocloud.v1.player.PlayerFindByNameRequest
import dev.httpmarco.polocloud.v1.player.PlayerFindByServiceRequest
import dev.httpmarco.polocloud.v1.player.PlayerFindRequest
import dev.httpmarco.polocloud.v1.player.PlayerFindResponse
import io.grpc.stub.StreamObserver

class PlayerGrpcService : PlayerControllerGrpc.PlayerControllerImplBase() {

    override fun findAll(request: PlayerFindRequest, responseObserver: StreamObserver<PlayerFindResponse>) {
        val builder = PlayerFindResponse.newBuilder()
        val playerStorage = Agent.playerStorage

        for (player in playerStorage.findAll()) {
            builder.addPlayers(player.to())
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
            builder.addPlayers(player.to())
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
            builder.addPlayers(player.to())
        }

        responseObserver.onNext(builder.build())
        responseObserver.onCompleted()
    }

    override fun playerCount(
        request: PlayerCountRequest,
        responseObserver: StreamObserver<PlayerCountResponse>
    ) {
        val count = Agent.playerStorage.playerCount()
        val response = PlayerCountResponse.newBuilder()
            .setCount(count)
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}