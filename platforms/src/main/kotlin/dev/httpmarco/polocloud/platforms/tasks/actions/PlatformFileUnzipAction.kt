package dev.httpmarco.polocloud.platforms.tasks.actions

import dev.httpmarco.polocloud.platforms.PlatformParameters
import dev.httpmarco.polocloud.platforms.tasks.PlatformTaskStep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.createDirectory
import kotlin.io.path.createParentDirectories
import kotlin.io.path.notExists
import kotlin.io.path.outputStream

@Serializable
@SerialName("PlatformFileUnzipAction")
class PlatformFileUnzipAction : PlatformAction() {
    override fun run(
        file: Path,
        step: PlatformTaskStep,
        environment: PlatformParameters
    ) {
        if (file.notExists()) {
            return
        }

        ZipFile(file.toFile()).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val newFile = file.parent.resolve(entry.name)
                try {
                    newFile.createParentDirectories()
                } catch (ex: Exception) {
                    println(ex.message)
                }
                if (entry.isDirectory) {
                    newFile.createDirectory()
                } else {

                    zip.getInputStream(entry).use { input ->
                        newFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }
}