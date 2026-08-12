package de.polocloud.shared.event.player

import de.polocloud.shared.event.Event
import kotlinx.serialization.Serializable

/**
 * Broadcast by the `players kick` CLI command to every bridge instance in the
 * cluster; only the one actually hosting [playerId] (if any) acts on it, mirrors
 * the existing tab-complete request/response round trip
 * ([de.polocloud.shared.event.terminal.TabCompleteRequestEvent]) — a broadcast +
 * local-filter shape, not a targeted RPC, since the cloud has no direct channel
 * back into a specific running proxy process.
 */
@Serializable
data class PlayerKickRequestEvent(
    val playerId: String,
    val reason: String,
) : Event
