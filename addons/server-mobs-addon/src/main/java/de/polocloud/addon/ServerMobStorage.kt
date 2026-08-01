package de.polocloud.addon

import de.polocloud.common.configuration.ConfigurationManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Persists attached [ServerMob]s to [file] as JSON, so a spawned mob doesn't need to be
 * re-added by hand on every restart. Reuses [ConfigurationManager]'s shared `Json`
 * instance for consistency with the rest of the project rather than introducing a
 * second JSON setup.
 */
class ServerMobStorage(private val file: Path) {

    @Serializable
    private data class Document(val mobs: List<ServerMob> = emptyList())

    fun load(): List<ServerMob> {
        if (!file.exists()) return emptyList()

        return runCatching {
            ConfigurationManager.json.decodeFromString<Document>(file.readText())
        }.getOrElse { Document() }.mobs
    }

    fun save(mobs: Collection<ServerMob>) {
        file.parent?.let(Files::createDirectories)
        file.toFile().writeText(ConfigurationManager.json.encodeToString(Document(mobs.toList())))
    }
}
