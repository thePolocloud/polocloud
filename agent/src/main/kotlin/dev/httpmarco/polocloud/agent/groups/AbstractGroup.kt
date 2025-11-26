package dev.httpmarco.polocloud.agent.groups

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.services.AbstractService
import dev.httpmarco.polocloud.platforms.Platform
import dev.httpmarco.polocloud.platforms.PlatformPool
import dev.httpmarco.polocloud.shared.groups.Group
import dev.httpmarco.polocloud.shared.platform.PlatformIndex
import java.nio.file.Path
import kotlin.io.path.Path
import dev.httpmarco.polocloud.shared.properties.PropertyHolder
import dev.httpmarco.polocloud.shared.template.Template
import dev.httpmarco.polocloud.v1.groups.GroupType

open class AbstractGroup(
    name: String,
    minMemory: Int,
    maxMemory: Int,
    minOnlineServices: Int,
    maxOnlineServices: Int,
    startThreshold: Double,
    platform: PlatformIndex,
    createdAt: Long,
    templates: List<Template>,
    properties: PropertyHolder
) :
    Group(
        name,
        minMemory,
        maxMemory,
        minOnlineServices,
        maxOnlineServices,
        startThreshold,
        platform,
        createdAt,
        templates,
        properties
    ) {

    fun update() {
        // update the group
        Agent.runtime.groupStorage().update(this)
    }

    fun serviceCount(): Int {
        return this.services().count()
    }

    fun platform(): Platform {
        return PlatformPool.find(platform.name)!!
    }

    fun services(): List<AbstractService> {
        return Agent.runtime.serviceStorage().findByGroup(this)
    }

    fun applicationPlatformFile(): Path {
        return Path("local/metadata/cache/${platform.name}/${platform.version}/${platform.name}-${platform.version}${platform().language.suffix()}")
    }

    fun shutdownAll() {
        Agent.runtime.serviceStorage().findByGroup(this).forEach { it.shutdown() }
    }

    fun playerCount(): Int {
        return services().sumOf { it.playerCount }
    }

    fun updateMinMemory(minMemory: Int) {
        this.minMemory = minMemory
    }

    fun updateMaxMemory(maxMemory: Int) {
        this.maxMemory = maxMemory
    }

    fun updateMinOnlineServices(minOnlineServices: Int) {
        this.minOnlineService = minOnlineServices
    }

    fun updateMaxOnlineServices(maxOnlineServices: Int) {
        this.maxOnlineService = maxOnlineServices
    }

    fun updateStartThreshold(startThreshold: Double) {
        this.startThreshold = startThreshold
    }

    fun startServices(amount: Int): List<AbstractService> {
        val startedServices = mutableListOf<AbstractService>()

        // todo duplicated code
        repeat(amount) {
            val service = Agent.runtime.factory().generateInstance(this)
            Agent.runtime.serviceStorage().deployAbstractService(service)
            Agent.runtime.factory().bootApplication(service)
            startedServices.add(service)
        }
        
        return startedServices
    }

    fun canStartServices(amount: Int): Boolean {
        val currentServices = this.serviceCount()
        val maxServices = this.maxOnlineService
        return maxServices == -1 || currentServices + amount <= maxServices
    }

    override fun equals(other: Any?): Boolean {
        return if (other is AbstractGroup) {
            this.name == other.name
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    fun isProxy(): Boolean {
        return platform().type == GroupType.PROXY
    }

    fun isServer(): Boolean {
        return platform().type == GroupType.SERVER
    }
}