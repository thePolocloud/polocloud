package dev.httpmarco.polocloud.platforms.metadata

import dev.httpmarco.polocloud.common.json.PRETTY_JSON
import dev.httpmarco.polocloud.platforms.JSON
import dev.httpmarco.polocloud.platforms.PLATFORM_PATH
import dev.httpmarco.polocloud.platforms.Platform
import dev.httpmarco.polocloud.platforms.PlatformPool
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
            PlatformTaskPool.attach(JSON.decodeFromString<PlatformTask>(Files.readString(it)))
        }
        return true
    }

    private fun readPlatformMetadata(): Boolean {
        val path = PLATFORM_PATH.resolve("platforms")

        if (!path.exists()) {
            return false
        }

        path.listDirectoryEntries().forEach {
            PlatformPool.attach(PRETTY_JSON.decodeFromString<Platform>(Files.readString(it)))
        }
        return true
    }
}