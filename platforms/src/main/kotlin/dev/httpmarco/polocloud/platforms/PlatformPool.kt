package dev.httpmarco.polocloud.platforms

import dev.httpmarco.polocloud.platforms.bridge.Bridge
import dev.httpmarco.polocloud.platforms.bridge.scanForBridges
import dev.httpmarco.polocloud.platforms.metadata.MetadataReader
import dev.httpmarco.polocloud.platforms.metadata.MetadataTranslator
import kotlin.io.path.Path

object PlatformPool {

    private val platformPool = ArrayList<Platform>()
    private val platformBridges = ArrayList<Bridge>()

    init {
        // read all bridges before we start the platform pool
        platformBridges.addAll(scanForBridges("local/libs"))

        // load all tasks bevor we start the platform pool
        MetadataTranslator.read()

        MetadataReader.combineData()
    }

    fun find(id: String): Platform? {
        return platformPool.firstOrNull { it.name == id }
    }

    fun attach(platform: Platform) {
        if (find(platform.name) != null) {
            throw IllegalArgumentException("Platform with name '${platform.name}' already exists.")
        }
        platformPool.add(platform)
    }

    fun versionSize(): Int {
        return platformPool.sumOf { it.versions.size }
    }

    fun size() = platformPool.size

    fun platforms() = platformPool

    fun findBindBridge(name: String): Bridge? {
        return platformBridges.firstOrNull { it.name == name }
    }

}