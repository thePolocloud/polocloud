package de.polocloud.shared.event.player

import de.polocloud.shared.event.Event
import de.polocloud.shared.player.CloudPlayer
import kotlinx.serialization.Serializable

/**
 * Fired by the node once a player has been removed from the network as the
 * result of a kick — either a backend server kick with no fallback available, or
 * an operator's `players kick` command — right after their [CloudPlayer] record
 * is removed. A player that simply quit fires [PlayerDisconnectEvent] instead,
 * never both.
 */
@Serializable
data class PlayerKickEvent(
    val player: CloudPlayer,
    val reason: String,
) : Event
