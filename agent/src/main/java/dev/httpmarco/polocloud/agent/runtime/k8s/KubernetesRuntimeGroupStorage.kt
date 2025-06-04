package dev.httpmarco.polocloud.agent.runtime.k8s

import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.runtime.RuntimeGroupStorage

class KubernetesRuntimeGroupStorage : RuntimeGroupStorage {

    override fun items(): List<Group> {
        return emptyList()
    }
}