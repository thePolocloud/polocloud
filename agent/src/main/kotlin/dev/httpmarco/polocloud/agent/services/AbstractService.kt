package dev.httpmarco.polocloud.agent.services

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.groups.AbstractGroup
import dev.httpmarco.polocloud.agent.runtime.local.LOCAL_FACTORY_PATH
import dev.httpmarco.polocloud.agent.runtime.local.LOCAL_STATIC_FACTORY_PATH
import dev.httpmarco.polocloud.agent.utils.IndexDetector
import dev.httpmarco.polocloud.agent.utils.PortDetector
import dev.httpmarco.polocloud.shared.events.definitions.service.ServiceChangePlayerCountEvent
import dev.httpmarco.polocloud.shared.events.definitions.service.ServiceChangeStateEvent
import dev.httpmarco.polocloud.shared.service.Service
import dev.httpmarco.polocloud.shared.service.ServiceInformation
import dev.httpmarco.polocloud.shared.template.Template
import dev.httpmarco.polocloud.v1.groups.GroupType
import dev.httpmarco.polocloud.v1.services.ServiceState
import java.nio.file.Path

abstract class AbstractService(
    name: String,
    index: Int,
    state: ServiceState,
    platformType: GroupType,
    environment: Map<String, String>,
    host: String,
    port: Int,
    templates: List<Template>,
    information: ServiceInformation,
    minMemory: Int,
    maxMemory: Int,
    ) : Service(
    name,
    index,
    state,
    platformType,
    environment,
    host,
    port,
    templates,
    information,
    minMemory,
    maxMemory
) {

    val path: Path = (if (isStatic()) LOCAL_STATIC_FACTORY_PATH else LOCAL_FACTORY_PATH).resolve(name())

    constructor(group: AbstractGroup) : this(
        group.name,
        IndexDetector.findIndex(group),
        ServiceState.PREPARING,
        group.platform().type,
        group.properties.all().map { it.key to it.value.toString() }.toMap(),
        if (group.isProxy()) "0.0.0.0" else "127.0.0.1",
        PortDetector.nextPort(group),
        group.templates,
        ServiceInformation(System.currentTimeMillis()),
        group.minMemory,
        group.maxMemory
    )

    fun group(): AbstractGroup {
        return Agent.runtime.groupStorage().find(groupName)!!
    }

    init {
        properties += group().properties.all().map { it.key to it.value.toString() }.toMap()
        Agent.eventProvider().call(ServiceChangeStateEvent(this))
    }

    fun isStatic(): Boolean {
        return properties["static"]?.toBoolean() ?: false
    }

    fun shutdown(shutdownCleanUp: Boolean = true) {
        Agent.runtime.factory().shutdownApplication(this, shutdownCleanUp)
    }

    fun executeCommand(command: String): Boolean {
        return Agent.runtime.expender().executeCommand(this, command)
    }

    fun logs(limit: Int = 100): List<String> {
        return Agent.runtime.expender().readLogs(this, limit)
    }

    fun updateMinMemory(minMemory: Int) {
        if (state == ServiceState.STARTING || state == ServiceState.ONLINE) {
            throw IllegalStateException("Cannot update minMemory while service is starting or online")
        }
        this.minMemory = minMemory
    }

    fun updateMaxMemory(maxMemory: Int) {
        if (state == ServiceState.STARTING || state == ServiceState.ONLINE) {
            throw IllegalStateException("Cannot update minMemory while service is starting or online")
        }
        this.maxMemory = maxMemory
    }

    fun updateMaxPlayerCount(maxPlayerCount: Int) {
        this.maxPlayerCount = maxPlayerCount
    }

    fun updatePlayerCount(playerCount: Int) {
        val oldPlayerCount = this.playerCount
        this.playerCount = playerCount

        if (this.state == ServiceState.ONLINE && oldPlayerCount != playerCount) {
            Agent.eventProvider().call(ServiceChangePlayerCountEvent(this))
        }
    }

    fun updateMotd(motd: String) {
        this.motd = motd
    }

    fun updateCpuUsage(cpuUsage: Double) {
        this.cpuUsage = cpuUsage
    }

    fun updateMemoryUsage(memoryUsage: Double) {
        this.memoryUsage = memoryUsage
    }
}