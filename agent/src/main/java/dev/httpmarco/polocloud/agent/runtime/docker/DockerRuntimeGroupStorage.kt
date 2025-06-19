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
}