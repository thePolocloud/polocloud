package dev.httpmarco.polocloud.platforms

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.nio.file.Path

@Serializable
class PlatformPool(val platforms: List<Platform>) {

    companion object {
        /**
         * Load all platforms
         */
        fun load(url: String): PlatformPool {
            val platformTable: PlatformTable = Json.decodeFromString<PlatformTable>(URI(url).toURL().readText())
            val platforms = mutableListOf<Platform>()
            val platformUrl = url.substringBeforeLast("/")

            platformTable.availableProxies.forEach { platforms.add(loadPlatform(platformUrl, it, "proxies")) }
            platformTable.availableServers.forEach { platforms.add(loadPlatform(platformUrl, it, "servers")) }

            return PlatformPool(platforms)
        }

        fun load(path: Path) : PlatformPool {
            // todo
            return PlatformPool(listOf())
        }

        private fun loadPlatform(url: String, name: String, type: String): Platform {
            return Json.decodeFromString<Platform>(URI("$url/$type/$name.json").toURL().readText())
        }
    }
}