package de.polocloud.node.communication.handler.player

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.communication.handler.CallerAuthorization
import de.polocloud.node.event.ClusterEventService
import de.polocloud.node.player.CloudPlayerMapper
import de.polocloud.node.player.CloudPlayerRepository
import de.polocloud.proto.UnregisterPlayerRequest
import de.polocloud.proto.UnregisterPlayerResponse
import de.polocloud.shared.event.player.PlayerDisconnectEvent
import de.polocloud.shared.event.player.PlayerKickEvent
import java.util.UUID

/**
 * Removes a player that left the network — called by the bridge on Velocity's
 * `DisconnectEvent`, either as a plain quit ([UnregisterPlayerRequest.getKicked] false)
 * or as the result of a kick (true). Fires exactly one of [PlayerDisconnectEvent] /
 * [PlayerKickEvent] accordingly, never both.
 */
class UnregisterPlayerServerHandler : GrpcServerHandler<UnregisterPlayerRequest, UnregisterPlayerResponse> {

    override suspend fun handle(request: UnregisterPlayerRequest, context: GrpcServerContext): UnregisterPlayerResponse {
        val player = CloudPlayerRepository.findById(UUID.fromString(request.playerId))
            ?: return UnregisterPlayerResponse.newBuilder().setSuccess(false).build()

        CallerAuthorization.requireSelfOrTrustedCaller(context, player.currentProxy)

        CloudPlayerRepository.delete(player)

        val shared = CloudPlayerMapper.toShared(player)
        if (request.kicked) {
            ClusterEventService.call(PlayerKickEvent(shared, request.reason))
        } else {
            ClusterEventService.call(PlayerDisconnectEvent(shared))
        }

        return UnregisterPlayerResponse.newBuilder().setSuccess(true).build()
    }
}
