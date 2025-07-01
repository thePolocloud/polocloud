package dev.httpmarco.polocloud.agent.runtime.docker

import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.runtime.RuntimeGroupStorage


class DockerRuntimeGroupStorage : RuntimeGroupStorage {

    override fun items(): List<Group> {
        return listOf()
    }

    override fun item(identifier: String): Group? {
        TODO("Not yet implemented")
    }

    override fun publish(group: Group) {
        TODO("Not yet implemented")
    }

    override fun destroy(group: Group) {
        TODO("Not yet implemented")
    }

    override fun present(identifier: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun update(group: Group) {
        TODO("Not yet implemented")
    }

    override fun reload() {
        TODO("Not yet implemented")
    }
}