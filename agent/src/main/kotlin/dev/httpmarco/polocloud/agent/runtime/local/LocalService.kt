package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.runtime.local.tracking.LocalOnlineTrack
import dev.httpmarco.polocloud.agent.runtime.local.tracking.LocalServiceLogTrack
import dev.httpmarco.polocloud.agent.services.Service
import kotlin.io.path.Path

class LocalService(group: Group, id: Int, hostname: String = "127.0.0.1") : Service(group, id, hostname) {

    private val logTracker = LocalServiceLogTrack(this)
    private val onlineTrack = LocalOnlineTrack(this)

    var process: Process? = null
    val path = Path("temp/${name()}")

    fun startTracking() {
        this.onlineTrack.start()
        this.logTracker.start()
    }

    fun stopTracking() {
        this.logTracker.close()
        this.onlineTrack.close()
    }

    fun logs() : List<String> {
        return logTracker.cachedLogs
    }
}