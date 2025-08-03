package dev.httpmarco.polocloud.agent.groups

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.services.Service
import dev.httpmarco.polocloud.platforms.Platform
import dev.httpmarco.polocloud.platforms.PlatformPool
import java.nio.file.Path
import kotlin.io.path.Path

open class Group(val data: GroupData) {

    fun update() {
        // update the group
        Agent.runtime.groupStorage().update(group = this)
    }

    fun serviceCount(): Int {
        return this.services().count()
    }

    fun platform(): Platform {
        return PlatformPool.find(data.platform.group)!!
    }

    fun services(): List<Service> {
        return Agent.runtime.serviceStorage().findServicesByGroup(this)
    }

    fun applicationPlatformFile(): Path {
        return Path("local/metadata/cache/${data.platform.group}/${data.platform.version}/${data.platform.group}-${data.platform.version}${platform().language.suffix()}")
    }

    fun shutdownAll() {
        Agent.runtime.serviceStorage().findServicesByGroup(this).forEach { it.shutdown() }
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Group) {
            this.data.name == other.data.name
        } else {
            false
        }
    }
}