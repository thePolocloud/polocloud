package de.polocloud.shared.event.player

import de.polocloud.shared.event.Event
import kotlinx.serialization.Serializable

/**
 * Broadcast by the `players send` CLI command to every bridge instance in the
 * cluster; only the one actually hosting [playerId] (if any) acts on it — see
 * [PlayerKickRequestEvent] for why this is a broadcast rather than a targeted RPC.
 */
@Serializable
data class PlayerSendRequestEvent(
    val playerId: String,
    val targetServer: String,
) : Event
