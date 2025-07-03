package dev.httpmarco.polocloud.platforms

import kotlinx.serialization.Serializable
import java.net.URI
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

@Serializable
data class Platform(
    val name: String,
    val type: PlatformType,
    val language: PlatformLanguage,
    val url: String,
    val arguments: List<String>,
    val versions: List<PlatformVersion>
) {

    fun prepare(version: String) {
        val path = Path("local/platforms/$name/$version/$name-$version.jar")
        val version = versions.stream().filter { it.version == version }.findFirst().orElseThrow()

        // load directory if not exists
        path.parent.createDirectories()

        if(path.notExists()) {
            URI(url.replace("%version%", version.version).replace("%buildId%", version.buildId.toString())).toURL().openStream().use { input ->
                path.toFile().outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}