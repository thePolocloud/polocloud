package dev.httpmarco.polocloud.platforms.tasks

import dev.httpmarco.polocloud.platforms.PlatformParameters
import dev.httpmarco.polocloud.platforms.ServerPlatformForwarding
import dev.httpmarco.polocloud.platforms.tasks.actions.PlatformAction
import java.nio.file.Path

class PlatformTaskStep(
    val name: String,
    val filename: String,
    val action: PlatformAction,
    // if a step is significantly different for forwarding support, this can be used
    val forwardingFilter: ServerPlatformForwarding? = null
) {

    fun run(servicePath: Path, environment: PlatformParameters) {
        this.action.run(servicePath.resolve(environment.modifyValueWithEnvironment(filename)), this, environment)
    }
}