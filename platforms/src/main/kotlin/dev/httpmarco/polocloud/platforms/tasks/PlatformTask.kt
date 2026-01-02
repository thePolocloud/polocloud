package dev.httpmarco.polocloud.platforms.tasks

import dev.httpmarco.polocloud.platforms.PlatformParameters
import dev.httpmarco.polocloud.platforms.ServerPlatformForwarding
import java.nio.file.Path

data class PlatformTask(val name: String, val steps: List<PlatformTaskStep>) {

    fun runTask(servicePath: Path, environment: PlatformParameters) {
        val forwarding = environment.getParameter<ServerPlatformForwarding>("forwarding")

        this.steps.forEach {
            if (it.forwardingFilter == null || it.forwardingFilter == forwarding) {
                it.run(servicePath, environment)
            }
        }
    }

}