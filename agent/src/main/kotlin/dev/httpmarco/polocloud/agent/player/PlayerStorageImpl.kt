package dev.httpmarco.polocloud.agent.player

import dev.httpmarco.polocloud.shared.service.Service
import dev.httpmarco.polocloud.v1.player.PlayerActorResponse
import java.util.UUID
import java.util.concurrent.CompletableFuture

private val cachedPlayers = mutableMapOf<UUID, AbstractPolocloudPlayer>()

class PlayerStorageImpl : PlayerStorage {
    override fun addPlayer(player: AbstractPolocloudPlayer) {
        cachedPlayers[player.uniqueId] = player
    }

    override fun removePlayer(uniqueId: UUID) {
        cachedPlayers.remove(uniqueId)
    }

    override fun findAll(): List<AbstractPolocloudPlayer> {
        return cachedPlayers.values.toList()
    }

    override fun findAllAsync(): CompletableFuture<List<AbstractPolocloudPlayer>> {
        return CompletableFuture.completedFuture(findAll())
    }

    override fun findByName(name: String): AbstractPolocloudPlayer? {
        return cachedPlayers.values.find { it.name.equals(name, ignoreCase = true) }
    }

    override fun findByNameAsync(name: String): CompletableFuture<AbstractPolocloudPlayer?> = CompletableFuture.completedFuture(findByName(name))

    override fun findByUniqueId(uniqueId: UUID) = cachedPlayers[uniqueId]

    override fun findByUniqueIdAsync(uniqueId: UUID): CompletableFuture<AbstractPolocloudPlayer?> {
        return CompletableFuture.completedFuture(findByUniqueId(uniqueId))
    }

    override fun findByService(serviceName: String): List<AbstractPolocloudPlayer> {
        return cachedPlayers.values.filter { it.currentServerName.equals(serviceName, ignoreCase = true) }
    }

    override fun findByServiceAsync(service: Service): CompletableFuture<List<AbstractPolocloudPlayer>> {
        return CompletableFuture.completedFuture(findByService(service.name()))
    }

    override fun playerCount(): Int = cachedPlayers.size

    override fun messagePlayer(
        uniqueId: UUID,
        message: String
    ): PlayerActorResponse {
        TODO("Not yet implemented")
    }

    override fun kickPlayer(
        uniqueId: UUID,
        reason: String
    ): PlayerActorResponse {
        TODO("Not yet implemented")
    }

    override fun connectPlayerToService(
        uniqueId: UUID,
        serviceName: String
    ): PlayerActorResponse {
        TODO("Not yet implemented")
    }


}