package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.runtime.RuntimeServiceStorage
import dev.httpmarco.polocloud.agent.services.Service
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class LocalRuntimeServiceStorage : RuntimeServiceStorage {

    private val services = CopyOnWriteArrayList<Service>()

    override fun deployService(service: Service) {
        this.services.add(service)
    }

    override fun findService(uniqueId: UUID): Service? {
        return this.services.stream().filter { it.uniqueId == uniqueId }.findFirst().orElse(null)
    }

    override fun findService(name: String): Service? {
        TODO("Not yet implemented")
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

    override fun dropService(service: Service) {
        this.services.remove(service)
    }
}