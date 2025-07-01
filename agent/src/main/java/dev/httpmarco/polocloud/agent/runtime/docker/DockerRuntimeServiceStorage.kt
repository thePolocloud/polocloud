package dev.httpmarco.polocloud.agent.runtime.docker

import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.runtime.RuntimeServiceStorage
import dev.httpmarco.polocloud.agent.services.Service
import java.util.UUID

class DockerRuntimeServiceStorage : RuntimeServiceStorage {
    override fun deployService(service: Service) {
        TODO("Not yet implemented")
    }

    override fun findService(uuid: UUID): Service? {
        TODO("Not yet implemented")
    }

    override fun findServiceByName(name: String): Service? {
        TODO("Not yet implemented")
    }

    override fun findServicesByGroup(group: Group): List<Service> {
        TODO("Not yet implemented")
    }

    override fun items(): List<Service> {
        TODO("Not yet implemented")
    }
}