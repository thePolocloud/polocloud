package dev.httpmarco.polocloud.agent.player

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.shared.events.definitions.PlayerJoinEvent
import dev.httpmarco.polocloud.shared.events.definitions.PlayerLeaveEvent

class PlayerListener {

    init {
        Agent.eventService.subscribe(PlayerJoinEvent::class.java) { event ->
            val player = event.player
            val abstractPlayer = AbstractPolocloudPlayer(
                name = player.name,
                uniqueId = player.uniqueId,
                currentServiceName = player.currentServiceName,
                currentProxyName = player.currentProxyName
            )

            Agent.playerStorage.addPlayer(abstractPlayer)
        }

        Agent.eventService.subscribe(PlayerLeaveEvent::class.java) { event ->
            Agent.playerStorage.removePlayer(event.player.uniqueId)
        }
    }
}