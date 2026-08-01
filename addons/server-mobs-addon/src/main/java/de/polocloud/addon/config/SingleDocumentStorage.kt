package de.polocloud.addon.config

import de.polocloud.common.configuration.ConfigurationManager
import de.polocloud.common.configuration.watcher.FileWatcher
import kotlinx.serialization.KSerializer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Persists a single JSON document of type [T] to [file], the way [de.polocloud.addon.ServerMobStorage]
 * persists a list of mobs. Generic (unlike [de.polocloud.addon.ServerMobStorage]) because this
 * addon now has two independent single-document configs — [de.polocloud.addon.messages.Messages]
 * and [de.polocloud.addon.display.MobDisplay] — that would otherwise duplicate this exact
 * load/save/watch dance.
 */
class SingleDocumentStorage<T>(private val file: Path, private val serializer: KSerializer<T>) {

    private var watcher: FileWatcher? = null

    /** Loads the persisted document, or seeds [file] with [default]'s result if it doesn't exist yet (or fails to parse). */
    fun load(default: () -> T): T {
        if (!file.exists()) {
            val defaults = default()
            save(defaults)
            return defaults
        }

        return runCatching {
            ConfigurationManager.json.decodeFromString(serializer, file.readText())
        }.getOrElse { default() }
    }

    fun save(value: T) {
        file.parent?.let(Files::createDirectories)
        file.toFile().writeText(ConfigurationManager.json.encodeToString(serializer, value))
    }

    /** Calls [onReload] whenever [file] is modified on disk, e.g. by an operator hand-editing it. */
    fun watch(onReload: () -> Unit) {
        watcher = FileWatcher(file, onReload)
    }

    fun stopWatching() = watcher?.stop()
}
