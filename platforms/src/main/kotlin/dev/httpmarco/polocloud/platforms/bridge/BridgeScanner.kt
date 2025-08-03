package dev.httpmarco.polocloud.platforms.bridge

import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.jar.JarFile


fun readJsonFromJar(jarFile: File, jsonFileName: String): String? {
    JarFile(jarFile).use { jar ->
        val entry = jar.getEntry(jsonFileName) ?: return null
        jar.getInputStream(entry).use { input ->
            return input.readBytes().toString(StandardCharsets.UTF_8)
        }
    }
}

fun scanForBridges(name: String): List<Bridge> {
    val bridges = mutableListOf<Bridge>()
    val directory = File(name)

    if (directory.isDirectory) {
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.extension == "jar") {
                val jsonContent = readJsonFromJar(file, "bridge.json")
                if (jsonContent != null) {
                    // Assuming a function parseJsonToBridge exists to convert JSON to Bridge object
                    val bridge = Json.decodeFromString<Bridge>(jsonContent)
                    bridge.path = file.toPath()
                    bridges.add(bridge)
                }
            }
        }
    }
    return bridges
}
