package dev.httpmarco.polocloud.agent.services

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.utils.PortDetector
import dev.httpmarco.polocloud.agent.utils.asStringMap
import dev.httpmarco.polocloud.v1.ServiceState

abstract class Service(val group: Group, val id: Int, val hostname: String = "127.0.0.1") {

    val port = PortDetector.nextPort(group)
    var state = ServiceState.PREPARING
    var playerCount = -1
    var maxPlayerCount = -1
    var properties = hashMapOf<String, String>()

    init {
        properties.putAll(group.data.properties.asStringMap())
    }

    fun name(): String {
        return "${group.data.name}-${id}"
    }

    fun shutdown(shutdownCleanUp : Boolean = true) {
        Agent.runtime.factory().shutdownApplication(this, shutdownCleanUp)
    }

    fun executeCommand(command: String): Boolean {
        return Agent.runtime.expender().executeCommand(this, command)
    }

    fun logs(limit: Int = 100): List<String> {
        return Agent.runtime.expender().readLogs(this, limit)
    }
}