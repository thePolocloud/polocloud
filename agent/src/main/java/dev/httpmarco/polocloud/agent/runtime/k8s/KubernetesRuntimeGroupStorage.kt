package dev.httpmarco.polocloud.agent.runtime.k8s

import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.runtime.RuntimeGroupStorage
import io.fabric8.kubernetes.client.KubernetesClient

class KubernetesRuntimeGroupStorage(private val kubeClient: KubernetesClient) : RuntimeGroupStorage {

    override fun items(): List<Group> {
        return kubeClient
            .resources(KubernetesGroup::class.java)
            .list()
            .getItems()
            .stream()
            .map({ group -> Group(group.spec) })
            .toList()
    }

    override fun item(identifier: String): Group? {
        TODO("Not yet implemented")
    }

    override fun publish(group: Group) {
        TODO("Not yet implemented")
    }

    override fun destroy(group: Group) {
        // todo testing
        kubeClient.resources(KubernetesGroup::class.java).withName(group.data.name).delete()
    }

    override fun present(identifier: String): Boolean {
        TODO("Not yet implemented")
    }
}