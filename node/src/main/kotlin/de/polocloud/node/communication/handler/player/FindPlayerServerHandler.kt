package de.polocloud.node.communication.handler.player

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.player.CloudPlayerMapper
import de.polocloud.node.player.CloudPlayerRepository
import de.polocloud.proto.FindPlayerRequest
import de.polocloud.proto.FindPlayerResponse
import java.util.UUID

class FindPlayerServerHandler : GrpcServerHandler<FindPlayerRequest, FindPlayerResponse> {

    override suspend fun handle(request: FindPlayerRequest, context: GrpcServerContext): FindPlayerResponse {
        val player = request.playerId.takeIf { it.isNotBlank() }
            ?.let { CloudPlayerRepository.findById(UUID.fromString(it)) }
            ?: request.name.takeIf { it.isNotBlank() }?.let { CloudPlayerRepository.findByName(it) }

        val builder = FindPlayerResponse.newBuilder().setFound(player != null)
        if (player != null) {
            builder.player = CloudPlayerMapper.toProto(player)
        }
        return builder.build()
    }
}
