package dev.httpmarco.polocloud.agent.services

import dev.httpmarco.polocloud.agent.groups.Group
import java.util.UUID

class Service(val group: Group) {

    val uuid = UUID.randomUUID()

    fun name() : String {
        return "${group.data.name}-1"
    }
}