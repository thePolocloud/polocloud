package de.polocloud.shared.event.player

import de.polocloud.shared.event.Event
import de.polocloud.shared.player.CloudPlayer
import kotlinx.serialization.Serializable

/**
 * Fired by the node once a player has been registered as connected — on Velocity,
 * this is the proxy's `LoginEvent` (authenticated, before a backend server is
 * chosen), so [player].currentServer is `null` at this point. Subscribe to
 * [PlayerSwitchEvent] for when a backend server is actually assigned.
 */
@Serializable
data class PlayerJoinEvent(
    val player: CloudPlayer,
) : Event
