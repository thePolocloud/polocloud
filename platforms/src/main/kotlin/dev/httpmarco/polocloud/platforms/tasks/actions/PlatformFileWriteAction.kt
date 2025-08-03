package dev.httpmarco.polocloud.platforms.tasks.actions

import dev.httpmarco.polocloud.platforms.PlatformParameters
import dev.httpmarco.polocloud.platforms.tasks.PlatformTaskStep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

@Serializable
@SerialName("PlatformFileWriteAction")
class PlatformFileWriteAction(val content: String) : PlatformAction() {

    override fun run(
        file: Path,
        step: PlatformTaskStep,
        environment: PlatformParameters
    ) {
        file.parent.createDirectories()

        Files.writeString(file, environment.modifyValueWithEnvironment(content))
    }
}