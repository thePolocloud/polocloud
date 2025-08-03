package dev.httpmarco.polocloud.platforms.tasks.actions

import dev.httpmarco.polocloud.platforms.PlatformParameters
import dev.httpmarco.polocloud.platforms.tasks.PlatformTaskStep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable
@SerialName("PlatformExecuteCommandAction")
class PlatformExecuteCommandAction(val command: String) : PlatformAction() {
    override fun run(
        file: Path,
        step: PlatformTaskStep,
        environment: PlatformParameters
    ) {
        val builder = ProcessBuilder()
        builder.command("sh", "-c", command)
        builder.directory(file.toFile())
        val process = builder.start()

        process.waitFor()
    }
}