package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.runtime.RuntimeServiceStorage
import dev.httpmarco.polocloud.agent.services.Service
import java.util.UUID

class LocalRuntimeServiceStorage : RuntimeServiceStorage {

    private val services = ArrayList<Service>()

    override fun deployService(service: Service) {
        this.services.add(service)
    }

    override fun findService(uuid: UUID): Service? {
        return this.services.stream().filter { it.uuid == uuid }.findFirst().orElse(null)
    }

    override fun findServiceByName(name: String): Service? {
        return this.services.stream().filter { it.name() == name }.findFirst().orElse(null)
    }

    override fun findServicesByGroup(group: Group): List<Service> {
        return this.services.stream()
            .filter { it.group == group }
            .toList()
    }

    override fun items() = services
}