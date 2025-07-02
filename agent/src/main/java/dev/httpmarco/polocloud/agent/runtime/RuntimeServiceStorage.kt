package dev.httpmarco.polocloud.agent.runtime

import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.services.Service
import java.util.UUID

interface RuntimeServiceStorage {

    fun deployService(service: Service)

    fun findService(uniqueId: UUID): Service?

    fun findService(name: String): Service?

    fun findServiceByName(name: String): Service?

    fun findServicesByGroup(group: Group) : List<Service>

    fun items(): List<Service>

    fun dropService(service: Service)

}