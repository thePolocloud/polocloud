package de.polocloud.moduleapi.config

import de.polocloud.moduleapi.ModuleNode
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes.Tag
import java.io.File

/**
 * A single YAML-backed config file for a module (`config.yml` in its
 * [de.polocloud.moduleapi.ModuleNode.dataFolder]).
 *
 * [T] needs a public no-arg constructor and mutable (`var`) properties — a Kotlin data
 * class where every property has a default satisfies both, e.g.:
 * ```
 * data class CloudflareConfig(var apiToken: String = "", var zoneId: String = "")
 * ```
 * Uses snakeyaml's plain JavaBean (de)serialization — the same library the node itself
 * uses for its own YAML files — so it stays a `.yml` you can hand-edit, not a class dump.
 */
class ModuleConfig<T : Any>(
    private val file: File,
    private val type: Class<T>,
    private val default: () -> T,
) {

    private val yaml = Yaml(DumperOptions().apply { defaultFlowStyle = DumperOptions.FlowStyle.BLOCK })

    var value: T = load()
        private set

    /** Re-reads [file] from disk, discarding any in-memory change not yet [save]d. */
    fun reload(): T {
        value = load()
        return value
    }

    /** Writes [newValue] (the current [value] by default) to [file]. */
    fun save(newValue: T = value): T {
        value = newValue
        file.parentFile?.mkdirs()
        file.writeText(yaml.dumpAs(newValue, Tag.MAP, DumperOptions.FlowStyle.BLOCK))
        return newValue
    }

    private fun load(): T {
        if (!file.isFile) {
            return save(default())
        }
        return file.inputStream().use { yaml.loadAs(it, type) }
    }
}

/** Reified sugar for [ModuleNode.config] — `node.config { MyConfig() }`. */
inline fun <reified T : Any> ModuleNode.config(noinline default: () -> T): ModuleConfig<T> =
    config(T::class.java, default)
