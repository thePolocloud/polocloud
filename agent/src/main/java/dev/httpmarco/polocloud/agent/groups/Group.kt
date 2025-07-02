package dev.httpmarco.polocloud.agent.groups

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.services.Service
import dev.httpmarco.polocloud.platforms.Platform
import java.nio.file.Path
import kotlin.io.path.Path

open class Group(val data: GroupData) {

    fun update() {
        // update the grou
        Agent.instance.runtime.groupStorage().update(group = this)
    }

    fun serviceCount() : Int {
        return this.services().count()
    }

    fun platform() : Platform? {
        return Agent.instance.platformPool.findPlatform(data.platform.group)
    }

    fun services() : List<Service> {
        return Agent.instance.runtime.serviceStorage().findServicesByGroup(this)
    }

    fun applicationPlatformFile() : Path {
        return Path("local/platforms/${data.platform.group}/${data.platform.version}/${data.platform.group}-${data.platform.version}.jar");
    }
}