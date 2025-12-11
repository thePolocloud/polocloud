package dev.httpmarco.polocloud.agent.player

import com.google.protobuf.Any
import com.google.protobuf.Empty
import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.v1.player.PlayerActorIdentifier
import dev.httpmarco.polocloud.v1.player.PlayerActorResponse
import dev.httpmarco.polocloud.v1.player.PlayerConnectActorRequest
import dev.httpmarco.polocloud.v1.player.PlayerControllerGrpc
import dev.httpmarco.polocloud.v1.player.PlayerCountResponse
import dev.httpmarco.polocloud.v1.player.PlayerFindByNameRequest
import dev.httpmarco.polocloud.v1.player.PlayerFindByServiceRequest
import dev.httpmarco.polocloud.v1.player.PlayerFindResponse
import dev.httpmarco.polocloud.v1.player.PlayerMessageActorRequest
import dev.httpmarco.polocloud.v1.proto.EventProviderOuterClass
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import java.util.UUID

class PlayerGrpcService : PlayerControllerGrpc.PlayerControllerImplBase() {

    override fun findAll(request: Empty, responseObserver: StreamObserver<PlayerFindResponse>) {
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

    override fun playerCount(request: Empty, responseObserver: StreamObserver<PlayerCountResponse>) {
        val count = Agent.playerStorage.playerCount()
        val response = PlayerCountResponse.newBuilder()
            .setCount(count)
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun messagePlayer(
        request: PlayerMessageActorRequest,
        responseObserver: StreamObserver<PlayerActorResponse>
    ) {
        TODO()
    }

    override fun kickPlayer(
        request: PlayerMessageActorRequest,
        responseObserver: StreamObserver<PlayerActorResponse>
    ) {
        TODO()
    }

    override fun connectPlayer(
        request: PlayerConnectActorRequest,
        responseObserver: StreamObserver<PlayerActorResponse>
    ) {
        val player = Agent.playerStorage.findByUniqueId(UUID.fromString(request.uniqueId))

        if (player == null) {
            responseObserver.onNext(
                PlayerActorResponse.newBuilder().setSuccess(false).setErrorMessage("Player is not online.").build()
            )
            responseObserver.onCompleted()
            return
        }

        val proxy = Agent.runtime.serviceStorage().find(player.currentProxyName)

        if(!(proxy != null && proxy.actorService.isActive())) {
            responseObserver.onNext(
                PlayerActorResponse.newBuilder().setSuccess(false).setErrorMessage("Player proxy server not found or proxy is invalid session type.").build()
            )
            responseObserver.onCompleted()
            return
        }

     //   TODO("testing")
        proxy.actorService.stream(Any.pack(request))
    }

    override fun actorStreaming(request: PlayerActorIdentifier, responseObserver: StreamObserver<Any>) {
        val service = Agent.runtime.serviceStorage().find(request.serviceName)

        if (service == null) {
            responseObserver.onCompleted()
            return
        }

        service.actorService.updateStream(responseObserver as ServerCallStreamObserver<Any>)
    }
}