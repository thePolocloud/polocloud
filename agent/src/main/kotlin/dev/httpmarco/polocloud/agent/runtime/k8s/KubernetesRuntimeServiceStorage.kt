package dev.httpmarco.polocloud.agent.runtime.k8s

import dev.httpmarco.polocloud.agent.runtime.RuntimeServiceStorage
import dev.httpmarco.polocloud.agent.services.AbstractService
import dev.httpmarco.polocloud.shared.service.SharedBootConfiguration
import dev.httpmarco.polocloud.v1.groups.GroupType
import dev.httpmarco.polocloud.v1.services.ServiceSnapshot
import java.util.concurrent.CompletableFuture

class KubernetesRuntimeServiceStorage : RuntimeServiceStorage<KubernetesService> {


    override fun findAll(): List<KubernetesService> {
        TODO("Not yet implemented")
    }

    override fun findAllAsync(): CompletableFuture<List<KubernetesService>> {
        TODO("Not yet implemented")
    }

    override fun find(name: String): KubernetesService? {
        TODO("Not yet implemented")
    }

    override fun findAsync(name: String): CompletableFuture<KubernetesService?> {
        TODO("Not yet implemented")
    }

    override fun findByType(type: GroupType): List<KubernetesService> {
        TODO("Not yet implemented")
    }

    override fun findByTypeAsync(type: GroupType): CompletableFuture<List<KubernetesService>> {
        TODO("Not yet implemented")
    }

    override fun findByGroup(group: dev.httpmarco.polocloud.shared.groups.Group): List<KubernetesService> {
        TODO("Not yet implemented")
    }

    override fun findByGroupAsync(group: dev.httpmarco.polocloud.shared.groups.Group): CompletableFuture<List<KubernetesService>> {
        TODO("Not yet implemented")
    }

    override fun findByGroup(group: String): List<KubernetesService> {
        TODO("Not yet implemented")
    }

    override fun findByGroupAsync(group: String): CompletableFuture<List<KubernetesService>> {
        TODO("Not yet implemented")
    }

    override fun bootInstanceWithConfiguration(
        name: String,
        configuration: SharedBootConfiguration
    ): ServiceSnapshot {
        TODO("Not yet implemented")
    }

    override fun shutdownService(name: String): ServiceSnapshot {
        TODO("Not yet implemented")
    }

    override fun deployService(service: KubernetesService) {
        TODO("Not yet implemented")
    }

    override fun dropService(service: KubernetesService) {
        TODO("Not yet implemented")
    }

    override fun implementedService(abstractService: AbstractService): KubernetesService {
        TODO("Not yet implemented")
    }
}