package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.RuntimeTemplates
import dev.httpmarco.polocloud.v1.GroupType
import dev.httpmarco.polocloud.v1.proto.GroupProvider
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

class LocalRuntimeTemplates : RuntimeTemplates<LocalService> {

    private val TEMPLATE_PATH = Path("local/templates")

    init {
        GroupType.entries.forEach {
            TEMPLATE_PATH.resolve("EVERY_$it").createDirectories()
        }
        // default template directory for all groups
        TEMPLATE_PATH.resolve("EVERY").createDirectories()
    }

    override fun bindTemplate(service: LocalService) {
        service.group.data.templates.forEach {
            val sourcePath = TEMPLATE_PATH.resolve(it)
            if (!Files.exists(sourcePath)) {
                sourcePath.createDirectories()
                // no template found, create empty directory
            }
            copyDirectory(sourcePath, service.path)
        }
    }

    override fun saveTemplate(template: String, service: LocalService) {
       copyDirectory(service.path, TEMPLATE_PATH.resolve(template))
    }

    fun copyDirectory(sourcePath: Path, targetPath: Path) {
        Files.walkFileTree(sourcePath, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                val targetDir = targetPath.resolve(sourcePath.relativize(dir))
                if (!Files.exists(targetDir)) {
                    Files.createDirectory(targetDir)
                }
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                try {
                    Files.copy(
                        file,
                        targetPath.resolve(sourcePath.relativize(file)),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                } catch (e: IOException) {
                    logger.warn("Cannot copy file ${file.fileName}: ${e.message}")
                }
                return FileVisitResult.CONTINUE
            }
        })
    }

}