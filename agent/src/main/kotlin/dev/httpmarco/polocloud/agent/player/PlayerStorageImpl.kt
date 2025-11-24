package dev.httpmarco.polocloud.agent.player

import dev.httpmarco.polocloud.shared.service.Service
import java.util.UUID
import java.util.concurrent.CompletableFuture

private val cachedPlayers = mutableMapOf<UUID, AbstractPolocloudPlayer>()
private val actor = PlayerActor()

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

    override fun findByNameAsync(name: String): CompletableFuture<AbstractPolocloudPlayer?> {
        return CompletableFuture.completedFuture(findByName(name))
    }

    override fun findByService(serviceName: String): List<AbstractPolocloudPlayer> {
        return cachedPlayers.values.filter { it.currentServiceName.equals(serviceName, ignoreCase = true) }
    }

    override fun findByServiceAsync(service: Service): CompletableFuture<List<AbstractPolocloudPlayer>> {
        return CompletableFuture.completedFuture(findByService(service.name()))
    }

    override fun playerCount() = cachedPlayers.size

    override fun playerActor() = actor
}