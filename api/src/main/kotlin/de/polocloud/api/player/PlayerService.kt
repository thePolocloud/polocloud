package de.polocloud.api.player

import de.polocloud.shared.player.CloudPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.future.future
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture

/**
 * Public entry point to the player API.
 *
 * Backed by a [PlayerApiClient] (gRPC in production). Obtain the shared instance via
 * [de.polocloud.api.Polocloud.playerService]. Mirrors
 * [de.polocloud.api.services.ServiceService]'s blocking/`*Async` split — see that
 * class's doc for when to use which form.
 */
class PlayerService internal constructor(
    private val client: PlayerApiClient,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Registers [player] as newly connected. */
    fun register(player: CloudPlayer): Boolean =
        runBlocking(Dispatchers.IO) { client.registerPlayer(player) }.success

    /** Non-blocking form of [register]. */
    fun registerAsync(player: CloudPlayer): CompletableFuture<Boolean> =
        scope.future { client.registerPlayer(player).success }

    /** Updates the current backend server of the player [playerId] is connected as. */
    fun updateServer(playerId: String, server: String): Boolean =
        runBlocking(Dispatchers.IO) { client.updatePlayerServer(playerId, server) }.success

    /** Non-blocking form of [updateServer]. */
    fun updateServerAsync(playerId: String, server: String): CompletableFuture<Boolean> =
        scope.future { client.updatePlayerServer(playerId, server).success }

    /** Removes the player [playerId] as a plain disconnect. */
    fun unregister(playerId: String): Boolean =
        runBlocking(Dispatchers.IO) { client.unregisterPlayer(playerId, kicked = false, reason = "") }.success

    /** Non-blocking form of [unregister]. */
    fun unregisterAsync(playerId: String): CompletableFuture<Boolean> =
        scope.future { client.unregisterPlayer(playerId, kicked = false, reason = "").success }

    /** Removes the player [playerId] as the result of a kick, with [reason]. */
    fun unregisterKicked(playerId: String, reason: String): Boolean =
        runBlocking(Dispatchers.IO) { client.unregisterPlayer(playerId, kicked = true, reason = reason) }.success

    /** Non-blocking form of [unregisterKicked]. */
    fun unregisterKickedAsync(playerId: String, reason: String): CompletableFuture<Boolean> =
        scope.future { client.unregisterPlayer(playerId, kicked = true, reason = reason).success }

    /** The player with the given [id] (UUID string), or `null` if not connected. */
    fun find(id: String): CloudPlayer? = runBlocking(Dispatchers.IO) { client.findPlayer(playerId = id) }

    /** Non-blocking form of [find]. */
    fun findAsync(id: String): CompletableFuture<CloudPlayer?> = scope.future { client.findPlayer(playerId = id) }

    /** The player with the given [name], or `null` if not connected. */
    fun findByName(name: String): CloudPlayer? = runBlocking(Dispatchers.IO) { client.findPlayer(name = name) }

    /** Non-blocking form of [findByName]. */
    fun findByNameAsync(name: String): CompletableFuture<CloudPlayer?> = scope.future { client.findPlayer(name = name) }

    /** Every player currently known to the connected node. */
    fun findAll(): List<CloudPlayer> = runBlocking(Dispatchers.IO) { client.listPlayers() }

    /** Non-blocking form of [findAll]. */
    fun findAllAsync(): CompletableFuture<List<CloudPlayer>> = scope.future { client.listPlayers() }

    /** Releases background resources backing the `*Async` methods. */
    fun close() = scope.cancel()
}
