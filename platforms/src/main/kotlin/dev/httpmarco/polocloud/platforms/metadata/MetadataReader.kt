package dev.httpmarco.polocloud.platforms.metadata

import dev.httpmarco.polocloud.platforms.PLATFORM_GSON
import dev.httpmarco.polocloud.platforms.PLATFORM_PATH
import dev.httpmarco.polocloud.platforms.Platform
import dev.httpmarco.polocloud.platforms.PlatformPool
import dev.httpmarco.polocloud.platforms.exceptions.DuplicatedPlatformActionException
import dev.httpmarco.polocloud.platforms.tasks.PlatformTask
import dev.httpmarco.polocloud.platforms.tasks.PlatformTaskPool
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.system.exitProcess

object MetadataReader {

    private val logger = LogManager.getLogger()

    fun combineData() {
        if (!this.readTasksMetadata() || !this.readPlatformMetadata()) {
            logger.error("Could not load platform metadata! This is required to run PoloCloud Platforms.")
            exitProcess(-1);
        }
    }

    private fun readTasksMetadata(): Boolean {
        val path = PLATFORM_PATH.resolve("tasks")

        if (!path.exists()) {
            return false
        }

        path.listDirectoryEntries().forEach {
            val task = PLATFORM_GSON.fromJson(Files.readString(it), PlatformTask::class.java)

            if(PlatformTaskPool.find(task.name) != null) {
                throw DuplicatedPlatformActionException(task.name)
            }

            PlatformTaskPool.attach(task)
        }
        return true
    }

    private fun readPlatformMetadata(): Boolean {
        val path = PLATFORM_PATH.resolve("platforms")

        if (!path.exists()) {
            return false
        }

        path.listDirectoryEntries().forEach {
            PlatformPool.attach(PLATFORM_GSON.fromJson(Files.readString(it), Platform::class.java))
        }
        return true
    }
}