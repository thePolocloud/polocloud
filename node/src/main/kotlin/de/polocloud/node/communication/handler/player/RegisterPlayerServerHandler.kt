package de.polocloud.node.communication.handler.player

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.communication.handler.CallerAuthorization
import de.polocloud.node.event.ClusterEventService
import de.polocloud.node.player.CloudPlayerMapper
import de.polocloud.node.player.CloudPlayerRepository
import de.polocloud.proto.RegisterPlayerRequest
import de.polocloud.proto.RegisterPlayerResponse
import de.polocloud.shared.event.player.PlayerJoinEvent

/** Registers a newly-connected player — called by the bridge on Velocity's `LoginEvent`. */
class RegisterPlayerServerHandler : GrpcServerHandler<RegisterPlayerRequest, RegisterPlayerResponse> {

    override suspend fun handle(request: RegisterPlayerRequest, context: GrpcServerContext): RegisterPlayerResponse {
        // A proxy may only register a player as connected through itself, never "on
        // behalf of" another proxy — mirrors requireSelfOrTrustedCaller's use elsewhere.
        CallerAuthorization.requireSelfOrTrustedCaller(context, request.player.currentProxy)

        check(request.player.id.isNotBlank()) { "Player id must not be blank" }
        check(request.player.name.isNotBlank()) { "Player name must not be blank" }

        val player = CloudPlayerMapper.toDomain(request.player)
        CloudPlayerRepository.save(player)
        ClusterEventService.call(PlayerJoinEvent(CloudPlayerMapper.toShared(player)))

        return RegisterPlayerResponse.newBuilder().setSuccess(true).build()
    }
}
