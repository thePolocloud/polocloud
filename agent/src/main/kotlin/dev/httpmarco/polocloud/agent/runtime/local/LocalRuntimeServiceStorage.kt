package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.runtime.RuntimeServiceStorage
import dev.httpmarco.polocloud.agent.services.AbstractService
import dev.httpmarco.polocloud.shared.groups.Group
import dev.httpmarco.polocloud.shared.service.SharedBootConfiguration
import dev.httpmarco.polocloud.v1.groups.GroupType
import dev.httpmarco.polocloud.v1.services.ServiceSnapshot
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

class LocalRuntimeServiceStorage : RuntimeServiceStorage<LocalService> {

    private val services = CopyOnWriteArrayList<LocalService>()

    override fun findAll(): List<LocalService> = this.services


    override fun findAllAsync() = CompletableFuture.completedFuture(findAll())

    override fun find(name: String): LocalService? {
        return this.services.stream()
            .filter { it.name() == name }
            .findFirst()
            .orElse(null)
    }

    override fun findAsync(name: String): CompletableFuture<LocalService?> =
        CompletableFuture.completedFuture<LocalService?>(find(name))


    override fun findByType(type: GroupType): List<LocalService> {
        TODO("Not yet implemented")
    }

    override fun findByTypeAsync(type: GroupType): CompletableFuture<List<LocalService>> {
        TODO("Not yet implemented")
    }

    override fun findByGroup(group: Group): List<LocalService> {
        return this.services.stream()
            .filter { it.group() == group }
            .toList()
    }

    override fun findByGroupAsync(group: Group): CompletableFuture<List<LocalService>> {
        TODO("Not yet implemented")
    }

    override fun findByGroup(group: String): List<LocalService> {
        TODO("Not yet implemented")
    }

    override fun findByGroupAsync(group: String): CompletableFuture<List<LocalService>> {
        TODO("Not yet implemented")
    }

    override fun bootInstanceWithConfiguration(
        name: String,
        configuration: SharedBootConfiguration
    ): ServiceSnapshot {
        TODO()
    }

    override fun shutdownService(name: String): ServiceSnapshot {
        return Agent.runtime.factory().shutdownApplication(find(name) ?: throw IllegalArgumentException("Service not found: $name"))
    }

    override fun deployService(service: LocalService) {
        this.services.add(service)
    }

    override fun dropService(service: LocalService) {
        this.services.remove(service)
    }

    override fun implementedService(abstractService: AbstractService): LocalService {
        return abstractService as? LocalService ?: throw IllegalArgumentException("AbstractService must be of type LocalService")
    }
}