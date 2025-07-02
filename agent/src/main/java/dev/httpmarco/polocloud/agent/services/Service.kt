package dev.httpmarco.polocloud.agent.services

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.groups.Group
import java.util.UUID
import kotlin.io.path.Path

class Service(val group: Group) {

    val uniqueId = UUID.randomUUID()
    val path = Path("temp/${name()}-${uniqueId}")

    var state = State.PREPARING
    var process: Process? = null

    fun name(): String {
        return "${group.data.name}-1"
    }

    fun shutdown() {
        Agent.instance.runtime.factory().shutdownApplication(this)
    }

    enum class State {
        PREPARING,
        STARTING,
        ONLINE,
        STOPPING,
        STOPPED,
    }
}