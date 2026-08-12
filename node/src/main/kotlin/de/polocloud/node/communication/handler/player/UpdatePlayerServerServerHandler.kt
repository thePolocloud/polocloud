package de.polocloud.node.communication.handler.player

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.communication.handler.CallerAuthorization
import de.polocloud.node.event.ClusterEventService
import de.polocloud.node.player.CloudPlayerMapper
import de.polocloud.node.player.CloudPlayerRepository
import de.polocloud.proto.UpdatePlayerServerRequest
import de.polocloud.proto.UpdatePlayerServerResponse
import de.polocloud.shared.event.player.PlayerSwitchEvent
import java.util.UUID

/** Updates a connected player's current backend server — called by the bridge on Velocity's `ServerConnectedEvent`. */
class UpdatePlayerServerServerHandler : GrpcServerHandler<UpdatePlayerServerRequest, UpdatePlayerServerResponse> {

    override suspend fun handle(request: UpdatePlayerServerRequest, context: GrpcServerContext): UpdatePlayerServerResponse {
        val player = CloudPlayerRepository.findById(UUID.fromString(request.playerId))
            ?: return UpdatePlayerServerResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Unknown player '${request.playerId}' — it may have already disconnected")
                .build()

        // A switch is reported by the proxy the player is still connected through (the
        // backend server itself never talks to the node) — same self-or-trusted rule as
        // RegisterPlayerServerHandler.
        CallerAuthorization.requireSelfOrTrustedCaller(context, player.currentProxy)

        val previousServer = player.currentServer
        val newServer = request.server.ifBlank { null }
        if (previousServer == newServer) {
            return UpdatePlayerServerResponse.newBuilder().setSuccess(true).build()
        }

        player.currentServer = newServer
        CloudPlayerRepository.save(player)
        ClusterEventService.call(PlayerSwitchEvent(CloudPlayerMapper.toShared(player), previousServer))

        return UpdatePlayerServerResponse.newBuilder().setSuccess(true).build()
    }
}
