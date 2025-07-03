package dev.httpmarco.polocloud.agent.services

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.utils.PortDetector
import java.util.UUID

abstract class Service(val group: Group) {

    val uniqueId = UUID.randomUUID()
    val port = PortDetector.nextPort(group)
    val hostname = "127.0.0.1"

    var state = State.PREPARING

    fun name(): String {
        return "${group.data.name}-1"
    }

    fun shutdown() {
        Agent.instance.runtime.factory().shutdownApplication(this)
    }

    fun executeCommand(command: String) {
        Agent.instance.runtime.expender().executeCommand(this, command)
    }

    enum class State {
        PREPARING,
        STARTING,
        ONLINE,
        STOPPING,
        STOPPED,
    }
}