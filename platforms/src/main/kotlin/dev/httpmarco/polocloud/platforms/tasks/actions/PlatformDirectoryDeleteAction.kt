package dev.httpmarco.polocloud.platforms.tasks.actions

import dev.httpmarco.polocloud.platforms.PlatformParameters
import dev.httpmarco.polocloud.platforms.tasks.PlatformTaskStep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

@Serializable
@SerialName("PlatformDirectoryDeleteAction")
class PlatformDirectoryDeleteAction : PlatformAction() {
    override fun run(
        file: Path,
        step: PlatformTaskStep,
        environment: PlatformParameters
    ) {
        if (file.exists() && file.isDirectory()) {
            deleteDirectory(file.toFile())
        }
    }

    private fun deleteDirectory(directory: File) {
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deleteDirectory(file)
                } else {
                    file.delete()
                }
            }
            directory.delete()
        }
    }
}