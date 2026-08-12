package de.polocloud.shared.event.player

import de.polocloud.shared.event.Event
import de.polocloud.shared.player.CloudPlayer
import kotlinx.serialization.Serializable

/**
 * Fired by the node once a player has left the network on their own (a normal
 * quit) — right after their [CloudPlayer] record is removed. A player removed as
 * the result of a kick fires [PlayerKickEvent] instead, never both.
 */
@Serializable
data class PlayerDisconnectEvent(
    val player: CloudPlayer,
) : Event
