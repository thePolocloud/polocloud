package dev.httpmarco.polocloud.agent.player

import dev.httpmarco.polocloud.shared.player.ActorResult
import dev.httpmarco.polocloud.shared.player.PolocloudPlayer
import dev.httpmarco.polocloud.shared.player.SharedPlayerActor

class PlayerActor : SharedPlayerActor {

    override fun message(
        player: PolocloudPlayer,
        message: String
    ): ActorResult {
        TODO("Not yet implemented")
    }

    override fun toServer(
        player: PolocloudPlayer,
        serverName: String
    ): ActorResult {
        TODO("Not yet implemented")
    }

    override fun kick(
        player: PolocloudPlayer,
        reason: String
    ): ActorResult {
        TODO("Not yet implemented")
    }
}