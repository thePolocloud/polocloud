package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.services.Service
import kotlin.io.path.Path

class LocalService(group: Group) : Service(group) {

    var process: Process? = null
    val path = Path("temp/${name()}-${uniqueId}")

}