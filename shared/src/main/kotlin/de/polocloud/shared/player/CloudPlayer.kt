package de.polocloud.shared.player

import de.polocloud.shared.property.Properties
import kotlinx.serialization.Serializable

/**
 * A connected player, as exposed through the public API and carried on cluster
 * player events.
 *
 * Lives in `shared` (not `api`) so both the node (which publishes it on
 * [de.polocloud.shared.event.player.PlayerJoinEvent] /
 * [de.polocloud.shared.event.player.PlayerSwitchEvent] /
 * [de.polocloud.shared.event.player.PlayerDisconnectEvent] /
 * [de.polocloud.shared.event.player.PlayerKickEvent]) and the api/bridge (which
 * consume it) can use the exact same type without depending on each other —
 * mirrors [de.polocloud.shared.service.Service].
 */
@Serializable
data class CloudPlayer(
    val id: String,
    val name: String,
    /** Base64 texture blob from the Mojang profile's `textures` property. */
    val skinValue: String,
    /** Signature for [skinValue], from the same `textures` property. */
    val skinSignature: String,
    /** The full, raw set of Mojang profile properties this player logged in with. */
    val properties: Properties,
    /** Cluster-wide service name of the proxy this player is connected through. */
    val currentProxy: String,
    /** Cluster-wide service name of the backend server this player is on, or `null` if not yet routed there. */
    val currentServer: String?,
)
