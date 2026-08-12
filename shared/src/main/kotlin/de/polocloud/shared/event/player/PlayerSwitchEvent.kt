package de.polocloud.shared.event.player

import de.polocloud.shared.event.Event
import de.polocloud.shared.player.CloudPlayer
import kotlinx.serialization.Serializable

/**
 * Fired by the node whenever a player's [CloudPlayer.currentServer] changes,
 * including the very first assignment (`null` -> a server) right after
 * [PlayerJoinEvent] — Velocity's `ServerConnectedEvent` fires for that case too.
 *
 * @param player the player with its current, already-updated server.
 * @param previousServer the server the player was on before, or `null` if this is
 *   the first assignment.
 */
@Serializable
data class PlayerSwitchEvent(
    val player: CloudPlayer,
    val previousServer: String?,
) : Event
