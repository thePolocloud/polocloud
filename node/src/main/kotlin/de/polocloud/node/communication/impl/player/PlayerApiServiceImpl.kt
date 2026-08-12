package de.polocloud.node.communication.impl.player

import de.polocloud.common.communication.server.executor.GrpcServerExecutor
import de.polocloud.node.communication.grpc.GrpcContextFactory
import de.polocloud.proto.FindPlayerRequest
import de.polocloud.proto.FindPlayerResponse
import de.polocloud.proto.ListPlayersRequest
import de.polocloud.proto.ListPlayersResponse
import de.polocloud.proto.PlayerApiServiceGrpcKt
import de.polocloud.proto.RegisterPlayerRequest
import de.polocloud.proto.RegisterPlayerResponse
import de.polocloud.proto.UnregisterPlayerRequest
import de.polocloud.proto.UnregisterPlayerResponse
import de.polocloud.proto.UpdatePlayerServerRequest
import de.polocloud.proto.UpdatePlayerServerResponse

/**
 * gRPC entry point of the API-facing `PlayerApiService`, hosted on
 * [de.polocloud.node.communication.grpc.ServiceGrpcEndpoint] — mirrors
 * [de.polocloud.node.communication.impl.services.ServiceApiServiceImpl]: every RPC
 * delegates to the shared [GrpcServerExecutor] so it runs through the same middleware
 * pipeline (auth, logging, error mapping) as every other SDK-facing request.
 */
class PlayerApiServiceImpl(
    private val executor: GrpcServerExecutor,
) : PlayerApiServiceGrpcKt.PlayerApiServiceCoroutineImplBase() {

    override suspend fun registerPlayer(request: RegisterPlayerRequest): RegisterPlayerResponse =
        executor.execute(request, GrpcContextFactory.fromGrpc())

    override suspend fun updatePlayerServer(request: UpdatePlayerServerRequest): UpdatePlayerServerResponse =
        executor.execute(request, GrpcContextFactory.fromGrpc())

    override suspend fun unregisterPlayer(request: UnregisterPlayerRequest): UnregisterPlayerResponse =
        executor.execute(request, GrpcContextFactory.fromGrpc())

    override suspend fun findPlayer(request: FindPlayerRequest): FindPlayerResponse =
        executor.execute(request, GrpcContextFactory.fromGrpc())

    override suspend fun listPlayers(request: ListPlayersRequest): ListPlayersResponse =
        executor.execute(request, GrpcContextFactory.fromGrpc())
}
