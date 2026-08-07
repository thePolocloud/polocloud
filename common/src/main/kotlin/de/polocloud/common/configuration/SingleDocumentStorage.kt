package de.polocloud.common.configuration

import de.polocloud.common.configuration.watcher.FileWatcher
import kotlinx.serialization.KSerializer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Persists a single JSON document of type [T] to [file], reusing [ConfigurationManager]'s
 * shared `Json` instance. For configs whose data folder is only known at runtime — a plugin's
 * `getDataFolder()`, a proxy's `@DataDirectory` — the static-path [ConfigurationFile]
 * annotation (and the [ConfigurationHolder] it drives via [ConfigurationManager.load]) can't
 * express that, so several addons (proxy-addon, server-mobs-addon, sign-system) previously each
 * rolled their own near-identical copy of this exact load/save/[watch] dance. This is the
 * shared implementation all of them now use.
 */
class SingleDocumentStorage<T>(private val file: Path, private val serializer: KSerializer<T>) {

    private var watcher: FileWatcher? = null

    /** The persisted document, or `null` if [file] doesn't exist yet or fails to parse. Unlike [load], never writes to disk. */
    fun readOrNull(): T? {
        if (!file.exists()) return null
        return runCatching { ConfigurationManager.json.decodeFromString(serializer, file.readText()) }.getOrNull()
    }

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
