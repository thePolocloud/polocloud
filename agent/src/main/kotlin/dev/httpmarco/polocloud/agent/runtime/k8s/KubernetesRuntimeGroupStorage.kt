package dev.httpmarco.polocloud.agent.runtime.k8s

import dev.httpmarco.polocloud.agent.groups.AbstractGroup
import dev.httpmarco.polocloud.agent.runtime.RuntimeGroupStorage
import dev.httpmarco.polocloud.shared.platform.PlatformIndex
import dev.httpmarco.polocloud.shared.properties.PropertyHolder
import io.fabric8.kubernetes.client.KubernetesClient
import java.util.concurrent.CompletableFuture

class KubernetesRuntimeGroupStorage(private val kubeClient: KubernetesClient) : RuntimeGroupStorage {

    override fun findAll(): List<AbstractGroup> {
        return kubeClient
            .resources(KubernetesGroup::class.java)
            .list()
            .getItems()
            .stream()
            // todo
            .map { group ->
                AbstractGroup(
                    group.name,
                    0,
                    0,
                    0,
                    0,
                    0.0,
                    PlatformIndex("", ""),
                    0,
                    emptyList(),
                    PropertyHolder.empty()
                )
            }
            .toList()
    }

    override fun findAllAsync(): CompletableFuture<List<AbstractGroup>> {
        TODO("Not yet implemented")
    }

    override fun find(name: String): AbstractGroup? {
        TODO("Not yet implemented")
    }

    override fun findAsync(name: String): CompletableFuture<AbstractGroup?> {
        TODO("Not yet implemented")
    }

    override fun create(group: AbstractGroup): AbstractGroup? {
        TODO("Not yet implemented")
    }

    override fun createAsync(group: AbstractGroup): CompletableFuture<AbstractGroup?> {
        TODO("Not yet implemented")
    }

    override fun update(group: AbstractGroup): AbstractGroup? {
        TODO("Not yet implemented")
    }

    override fun updateAsync(group: AbstractGroup): CompletableFuture<AbstractGroup?> {
        TODO("Not yet implemented")
    }

    override fun delete(name: String): Boolean {
        TODO("Not yet implemented")
    }
}