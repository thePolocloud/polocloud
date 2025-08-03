package dev.httpmarco.polocloud.platforms.tasks

import dev.httpmarco.polocloud.platforms.PlatformParameters
import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable
data class PlatformTask(val name: String, val steps: List<PlatformTaskStep>) {

    fun runTask(servicePath: Path, environment: PlatformParameters) {
        this.steps.forEach {
            it.run(servicePath, environment)
        }
    }
}