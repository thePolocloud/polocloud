package dev.httpmarco.polocloud.platforms.metadata

import dev.httpmarco.polocloud.platforms.PLATFORM_GSON
import dev.httpmarco.polocloud.platforms.PLATFORM_PATH
import dev.httpmarco.polocloud.platforms.Platform
import dev.httpmarco.polocloud.platforms.PlatformPool
import dev.httpmarco.polocloud.platforms.exceptions.DuplicatedPlatformActionException
import dev.httpmarco.polocloud.platforms.exceptions.PlatformMetadataNotLoadableException
import dev.httpmarco.polocloud.platforms.tasks.PlatformTask
import dev.httpmarco.polocloud.platforms.tasks.PlatformTaskPool
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

object MetadataReader {

    fun combineData() {
        if (!this.readTasksMetadata() || !this.readPlatformMetadata()) {
            throw PlatformMetadataNotLoadableException()
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