package de.polocloud.node.communication.handler.player

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.player.CloudPlayerMapper
import de.polocloud.node.player.CloudPlayerRepository
import de.polocloud.proto.ListPlayersRequest
import de.polocloud.proto.ListPlayersResponse

class ListPlayersServerHandler : GrpcServerHandler<ListPlayersRequest, ListPlayersResponse> {

    override suspend fun handle(request: ListPlayersRequest, context: GrpcServerContext): ListPlayersResponse =
        ListPlayersResponse.newBuilder()
            .addAllPlayers(CloudPlayerRepository.findAll().map(CloudPlayerMapper::toProto))
            .build()
}
