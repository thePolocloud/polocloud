package dev.httpmarco.polocloud.platforms.metadata

import com.google.gson.JsonObject
import dev.httpmarco.polocloud.platforms.PLATFORM_GSON
import dev.httpmarco.polocloud.platforms.PLATFORM_METADATA_URL
import dev.httpmarco.polocloud.platforms.PLATFORM_PATH
import org.apache.logging.log4j.LogManager
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.util.zip.ZipFile
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.system.exitProcess

/**
 * Responsible for loading platform metadata from GitHub, classpath resources, or local cache.
 * Ensures that metadata is always available at runtime in the local storage directory.
 */
object MetadataTranslator {

    private val logger = LogManager.getLogger()

    /**
     * Tries to load metadata from local disk, GitHub, or classpath.
     * Will throw an exception if no metadata source is available.
     */
    fun read() {
        if (!(PLATFORM_PATH.resolve("tasks").exists() && PLATFORM_PATH.resolve("platforms").exists())) {
            if (!copyFromGitHub() && !copyFromClasspath()) {
                logger.error("Could not load platform metadata! This is required to run PoloCloud Platforms.")
                exitProcess(-1);
            }
        }
    }

    /**
     * Copies metadata files from classpath resources to local storage.
     * Supports running from both IDE (unpacked) and JAR (packed) contexts.
     *
     * @return true if the copy was successful, false otherwise.
     */
    fun copyFromClasspath(): Boolean {
        val components = listOf("tasks", "platforms")

        for (component in components) {
            val dirPath = "metadata/$component"
            val dirUrl = javaClass.classLoader.getResource(dirPath) ?: continue

            when (dirUrl.protocol) {
                "jar" -> copyComponentFromJar(dirPath, component)
                "file" -> copyComponentFromDirectory(File(dirUrl.toURI()), component)
                else -> continue
            }
        }
        return true
    }

    /**
     * Copies the metadata files from GitHub into the local platform directory.
     *
     * @return true if GitHub metadata was successfully downloaded, false otherwise.
     */
    private fun copyFromGitHub(): Boolean {
        val metadataUrl = PLATFORM_METADATA_URL + "metadata.json"

        val jsonText = URI(metadataUrl).toURL().openStream().bufferedReader(Charsets.UTF_8).readText()
        val jsonContext = PLATFORM_GSON.fromJson(jsonText, JsonObject::class.java)

        val tasks = jsonContext["tasks"]?.asJsonArray ?: return false
        val platforms = jsonContext["platforms"]?.asJsonArray ?: return false

        tasks.forEach {
            val fileName = it.asJsonPrimitive.asString
            downloadAndWriteToLocal("tasks", fileName)
        }

        platforms.forEach {
            val fileName = it.asJsonPrimitive.asString
            downloadAndWriteToLocal("platforms", fileName)
        }
        return true
    }

    /**
     * Downloads a single metadata file and writes it to the local platform directory.
     */
    private fun downloadAndWriteToLocal(type: String, fileName: String) {
        val remoteUrl = "$PLATFORM_METADATA_URL$type/$fileName.json"
        val targetPath = PLATFORM_PATH.resolve(type).resolve("$fileName.json")

        targetPath.parent.createDirectories()
        val content = URI(remoteUrl).toURL().readText()
        Files.writeString(targetPath, content)
    }

    /**
     * Extracts and copies a resource directory from a JAR archive into local storage.
     */
    private fun copyComponentFromJar(dirPath: String, targetSubPath: String) {
        val jarPath = javaClass.classLoader.getResource(dirPath)!!.path
            .substringBefore("!")
            .removePrefix("file:")

        ZipFile(jarPath).use { zip ->
            zip.entries().asSequence()
                .filter { it.name.startsWith(dirPath) && !it.isDirectory }
                .forEach { entry ->
                    val inputStream = zip.getInputStream(entry)
                    val relativePath = entry.name.removePrefix("metadata/")
                    val outFile = PLATFORM_PATH.resolve(relativePath).toFile()

                    outFile.parentFile.mkdirs()
                    outFile.outputStream().use { inputStream.copyTo(it) }
                }
        }
    }

    /**
     * Recursively copies a directory from unpacked classpath (e.g. running from IDE).
     */
    private fun copyComponentFromDirectory(sourceDir: File, targetSubPath: String) {
        val targetDir = PLATFORM_PATH.resolve(targetSubPath).toFile()
        sourceDir.copyRecursively(targetDir, overwrite = true)
    }
}
