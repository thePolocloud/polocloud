package de.polocloud.common.configuration

import de.polocloud.common.configuration.ConfigurationManager.load
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.reflect.KClass

/**
 * Central registry for all configs.
 *
 * Provides two APIs:
 * - [load]        → simple usage, reports + throws if necessary
 * - [loadResult]  → full control via [PoloResult]
 *
 * Call once at startup to initialize configs.
 */
object ConfigurationManager {

    val configs = mutableMapOf<KClass<*>, ConfigurationHolder<*>>()

    val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * One-shot read of [path] via [serializer], without registering it in [configs] or
     * watching it for changes — unlike [load], which requires a `@ConfigurationFile`
     * annotated class. Used for configs owned by something other than this process (e.g.
     * the CLI's `Cli.resolveInitialConfiguration` reads a separate installer-generated
     * config once, on first boot, to seed the CLI's own initial configuration).
     *
     * @param path
     * @param serializer
     */
    fun <T> read(path: Path, serializer: KSerializer<T>): T? {
        return if (path.exists()) {
            val content = Files.readString(path)
            json.decodeFromString(serializer, content)
        } else {
            null
        }
    }

    inline fun <reified T : Any> load(): ConfigurationHolder<T> {
        val annotation = T::class.java.getAnnotation(ConfigurationFile::class.java)
            ?: throw IllegalStateException("Missing @ConfigurationFile on ${T::class.simpleName}")

        val ser = serializer<T>()
        val holder = ConfigurationHolder(T::class, annotation.path, json, ser)

        holder.init()

        configs[T::class] = holder
        return holder
    }

    /**
     * Stops all file watchers. Call on application shutdown.
     */
    fun shutdown() {
        configs.values.forEach { it.stopWatcher() }
        configs.clear()
    }
}