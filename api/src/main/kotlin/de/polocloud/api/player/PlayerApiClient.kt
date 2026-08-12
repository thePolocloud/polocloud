package de.polocloud.api.player

import de.polocloud.shared.player.CloudPlayer

/**
 * Transport-agnostic gateway to the node's player API.
 *
 * Implemented by [GrpcPlayerApiClient] for real gRPC traffic; abstracted as an
 * interface so [PlayerService] can be unit-tested without a live node. Works
 * directly with the shared [CloudPlayer] model rather than the protobuf wire type
 * — unlike [de.polocloud.api.services.ServiceApiClient], there is no separate
 * api-layer mapper: [GrpcPlayerApiClient] converts to/from proto internally.
 */
interface PlayerApiClient {

    /** Registers [player] as newly connected. */
    suspend fun registerPlayer(player: CloudPlayer): PlayerCommandResult

    /** Updates the current backend server of the player [playerId] is connected as. */
    suspend fun updatePlayerServer(playerId: String, server: String): PlayerCommandResult

    /** Removes the player [playerId], either as a plain disconnect or (if [kicked]) with [reason]. */
    suspend fun unregisterPlayer(playerId: String, kicked: Boolean, reason: String): PlayerCommandResult

    /** The player matching [playerId] or [name] (whichever is non-null; id checked first), or `null`. */
    suspend fun findPlayer(playerId: String? = null, name: String? = null): CloudPlayer?

    /** Every player currently known to the connected node. */
    suspend fun listPlayers(): List<CloudPlayer>
}

/** Outcome of a [PlayerApiClient] write call. */
data class PlayerCommandResult(val success: Boolean, val message: String)
