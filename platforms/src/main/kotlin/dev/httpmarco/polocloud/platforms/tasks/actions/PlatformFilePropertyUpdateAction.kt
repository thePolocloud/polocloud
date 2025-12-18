package dev.httpmarco.polocloud.platforms.tasks.actions

import dev.httpmarco.polocloud.platforms.PlatformParameters
import dev.httpmarco.polocloud.platforms.tasks.PlatformTaskStep
import dev.httpmarco.polocloud.platforms.tasks.TaskFileMode
import org.tomlj.TomlTable
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import org.yaml.snakeyaml.Yaml
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class PlatformFilePropertyUpdateAction(
    private val key: String,
    private val value: String,
    private val fileMode : TaskFileMode = TaskFileMode.EVERY
) : PlatformAction() {

    override fun run(file: Path, step: PlatformTaskStep, environment: PlatformParameters) {

        if(fileMode == TaskFileMode.IF_EXISTS && file.notExists()) {
            return
        }

        if(fileMode == TaskFileMode.IF_NOT_EXISTS && file.exists()) {
            return
        }

        file.parent.createDirectories()
        val translatedValue = environment.modifyValueWithEnvironment(value)
        val parsedValue = parseValue(translatedValue)

        when {
            step.filename.endsWith(".properties") -> handleProperties(file, parsedValue.toString())
            step.filename.endsWith(".yml") -> handleYaml(file, parsedValue)
            step.filename.endsWith(".toml") -> handleToml(file, parsedValue)
        }
    }

    private fun handleProperties(file: Path, value: String) {
        val properties = Properties().apply {
            if (file.exists()) {
                FileInputStream(file.toFile()).use { load(it) }
            }
            setProperty(key, value)
        }

        FileOutputStream(file.toFile()).use {
            properties.store(it, "Updated configuration property: $key")
        }
    }

    private fun handleYaml(file: Path, value: Any?) {
        if (isNukkitConfig(file)) {
            handleSnakeYaml(file, value)
            return
        }

        handleConfigurateYaml(file, value)
    }

    private fun handleConfigurateYaml(file: Path, value: Any?) {
        val loader = YamlConfigurationLoader.builder().path(file).build()
        val root = if (file.exists()) loader.load() else loader.createNode()
        val path: Array<Any> =
            key.split(".")
                .map { it.toIntOrNull() ?: it }
                .toTypedArray()

        root.node(*path).set(value)
        loader.save(root)
    }

    private fun handleSnakeYaml(file: Path, value: Any?) {
        val yaml = Yaml()
        val data: MutableMap<String, Any?> = if (file.exists()) {
            FileReader(file.toFile()).use { yaml.load(it) as? MutableMap<String, Any?> } ?: mutableMapOf()
        } else {
            mutableMapOf()
        }

        data.withNestedValue(key.split("."), value)

        FileWriter(file.toFile()).use { yaml.dump(data, it) }
    }

    private fun handleToml(file: Path, value: Any?) {
        val content = if (file.exists()) file.readText().lines().toMutableList() else mutableListOf()
        val lineIndex = content.indexOfFirst { it.trim().startsWith("$key =") }

        if (lineIndex >= 0) {
            content[lineIndex] = "$key = \"$value\""
        } else {
            content.add("$key = \"$value\"")
        }

        if(file.notExists()) {
            file.createFile();
        }

        file.writeText(content.joinToString("\n"))
    }

    private fun parseValue(input: String): Any = when {
        input.equals("true", ignoreCase = true) -> true
        input.equals("false", ignoreCase = true) -> false
        input.toIntOrNull() != null -> input.toInt()
        else -> input
    }


    private fun MutableMap<String, Any?>.withNestedValue(keys: List<String>, value: Any?): MutableMap<String, Any?> {
        if (keys.isEmpty()) return this

        val key = keys.first()
        if (keys.size == 1) {
            this[key] = value
        } else {
            val child = when (val current = this[key]) {
                is TomlTable -> current.toMap().toMutableMap()
                is MutableMap<*, *> -> current as MutableMap<String, Any?>
                else -> mutableMapOf()
            }
            this[key] = child.withNestedValue(keys.drop(1), value)
        }
        return this
    }

    private fun isNukkitConfig(file: Path): Boolean {
        return file.fileName.toString().equals("nukkit.yml", ignoreCase = true)
    }
}