package dev.httpmarco.polocloud.platforms.tasks.actions

import dev.httpmarco.polocloud.platforms.PlatformParameters
import dev.httpmarco.polocloud.platforms.tasks.PlatformTaskStep
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable
@Polymorphic
abstract class PlatformAction() {

    abstract fun run(file: Path, step: PlatformTaskStep, environment: PlatformParameters)


}