package dev.httpmarco.polocloud.platforms.tasks

import dev.httpmarco.polocloud.platforms.PlatformParameters
import dev.httpmarco.polocloud.platforms.tasks.actions.PlatformAction
import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable
class PlatformTaskStep(val name: String, val description: String, val filename: String, val action: PlatformAction) {

    fun run(servicePath: Path, environment: PlatformParameters) {
        this.action.run(servicePath.resolve(filename), this, environment)
    }

}