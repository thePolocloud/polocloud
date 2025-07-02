package dev.httpmarco.polocloud.platforms

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

private const val PLATFORM_TABLE_URL =
    "https://raw.githubusercontent.com/HttpMarco/polocloud/refs/heads/master/metadata/metadata.json"


@Serializable
class PlatformPool(val platforms: List<Platform>, val path: Path) {

    companion object {
        private fun loadPlatform(url: String, name: String, type: String): Platform {
            return Json.decodeFromString<Platform>(URI("$url/$type/$name.json").toURL().readText())
        }

        fun load(path: Path): PlatformPool {
            if (path.exists()) {
                return PlatformPool(PRETTY_JSON.decodeFromString(Files.readString(path)), path)
            } else {
                val platformTable: PlatformTable =
                    Json.decodeFromString<PlatformTable>(URI(PLATFORM_TABLE_URL).toURL().readText())
                val platforms = mutableListOf<Platform>()
                val platformUrl = PLATFORM_TABLE_URL.substringBeforeLast("/")

                platformTable.availableProxies.forEach { platforms.add(loadPlatform(platformUrl, it, "proxies")) }
                platformTable.availableServers.forEach { platforms.add(loadPlatform(platformUrl, it, "servers")) }

                val platformPool = PlatformPool(platforms, path)
                platformPool.saveLocal()

                return platformPool
            }
        }
    }


    fun saveLocal() {
        Files.writeString(path, PRETTY_JSON.encodeToString(platforms))
    }
}