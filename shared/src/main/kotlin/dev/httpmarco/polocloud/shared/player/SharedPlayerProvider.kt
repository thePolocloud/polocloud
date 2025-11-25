package dev.httpmarco.polocloud.shared.player

import dev.httpmarco.polocloud.shared.service.Service
import java.util.concurrent.CompletableFuture

/**
 * Interface for a shared player provider that allows interaction with players in the cloud.
 */
interface SharedPlayerProvider<P : PolocloudPlayer> {

    /**
     * Returns a list of all connected players in the cloud.
     */
    fun findAll(): List<P>

    /**
     * Asynchronously returns all connected players.
     */
    fun findAllAsync(): CompletableFuture<List<P>>

    /**
     * Finds a player by their exact name.
     */
    fun findByName(name: String): P?

    /**
     * Asynchronously finds a player by name.
     */
    fun findByNameAsync(name: String): CompletableFuture<P?>

    /**
     * Returns all players currently on a specific service.
     */
    fun findByService(serviceName: String): List<P>

    /**
     * Asynchronously returns all players on a service.
     */
    fun findByServiceAsync(service: Service): CompletableFuture<List<P>>

    fun playerCount(): Int

    fun message(player: PolocloudPlayer, message: String)

    fun kick(player: PolocloudPlayer, message: String)

    fun toServer(player: PolocloudPlayer, serviceName: String)
}