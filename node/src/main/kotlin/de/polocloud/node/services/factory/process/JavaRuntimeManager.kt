package de.polocloud.service.factory.process

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.util.zip.ZipInputStream

/**
 * Downloads and manages local JRE installations for specific Java major versions.
 *
 * Runtimes are sourced from the [Adoptium API](https://adoptium.net) (Eclipse Temurin)
 * and cached under `runtimes/java-{version}/`. The OS and CPU architecture are
 * detected automatically at runtime.
 */
object JavaRuntimeManager {

    private val logger = LoggerFactory.getLogger(JavaRuntimeManager::class.java)
    private val runtimesDir = File("runtimes")

    /**
     * Ensures the JRE for [javaVersion] is available locally.
     * Downloads and extracts it from Adoptium if not yet cached.
     *
     * @param javaVersion Java major version required (e.g. 8, 17, 21).
     * @return The java executable [File] for the requested version.
     * @throws IllegalStateException if the executable cannot be located after extraction.
     */
    @Synchronized
    fun ensure(javaVersion: Int): File {
        val runtimeDir = File(runtimesDir, "java-$javaVersion")
        findExecutable(runtimeDir)?.let { return it }

        findSystemJava(javaVersion)?.let {
            logger.info("Using system-installed Java {} at {}", javaVersion, it.absolutePath)
            return it
        }

        logger.info("Java {} not found — fetching from Adoptium ...", javaVersion)
        val (downloadUrl, fileName) = fetchDownloadInfo(javaVersion)
        val archive = File(runtimeDir.also { it.mkdirs() }, fileName)

        logger.info("↓ Downloading {} ...", fileName)
        URI(downloadUrl).toURL().openStream().use { input ->
            archive.outputStream().use { input.copyTo(it) }
        }
        logger.info("↓ Downloaded {} ({} MB)", fileName, archive.length() / 1024 / 1024)

        extractArchive(archive, runtimeDir)
        archive.delete()

        return findExecutable(runtimeDir)
            ?: error("Java $javaVersion executable not found after extraction in ${runtimeDir.absolutePath}")
    }

    /**
     * Queries the Adoptium v3 API for the latest JRE of [javaVersion] matching
     * the current OS and architecture.
     *
     * @return Pair of (download URL, file name).
     */
    private fun fetchDownloadInfo(javaVersion: Int): Pair<String, String> {
        val url = "https://api.adoptium.net/v3/assets/latest/$javaVersion/hotspot" +
                "?os=${detectOs()}&architecture=${detectArch()}&image_type=jre"
        val root = Json.parseToJsonElement(URI(url).toURL().readText()).jsonArray
        // Safe-navigate every step instead of chained `!!`: an unexpected Adoptium API
        // response (shape change, rate-limit error body, empty result for an unsupported
        // version/arch combination) should fail with a message that says what's missing,
        // not a bare NPE pointing at this line.
        val asset = root.firstOrNull()?.jsonObject
            ?: error("Unexpected Adoptium API response for Java $javaVersion: no matching asset returned")
        val pkg = asset["binary"]?.jsonObject?.get("package")?.jsonObject
            ?: error("Unexpected Adoptium API response for Java $javaVersion: missing 'binary.package' field")
        val link = pkg["link"]?.jsonPrimitive?.content
            ?: error("Unexpected Adoptium API response for Java $javaVersion: missing 'package.link' field")
        val name = pkg["name"]?.jsonPrimitive?.content
            ?: error("Unexpected Adoptium API response for Java $javaVersion: missing 'package.name' field")
        return link to name
    }

    /**
     * Searches [dir] recursively for the java executable inside any `bin/` subdirectory.
     * Sets the executable bit on non-Windows systems.
     *
     * @return The executable [File], or null if [dir] does not exist or contains no match.
     */
    private fun findExecutable(dir: File): File? {
        if (!dir.exists()) return null
        val execName = if (isWindows()) "java.exe" else "java"
        return dir.walkTopDown()
            .firstOrNull { it.name == execName && it.parentFile?.name == "bin" }
            ?.also { if (!isWindows()) it.setExecutable(true) }
    }

    /**
     * Looks for an already-installed JRE/JDK matching [javaVersion] via `JAVA_HOME`
     * and the `PATH`, so a machine that already has the right Java doesn't get a
     * redundant private copy downloaded from Adoptium into `runtimes/`.
     *
     * @return The matching java executable [File], or null if none of the candidates
     * exist or none report the requested major version.
     */
    private fun findSystemJava(javaVersion: Int): File? {
        val execName = if (isWindows()) "java.exe" else "java"

        val javaHomeCandidate = System.getenv("JAVA_HOME")?.let { File(it, "bin/$execName") }
        val pathCandidates = System.getenv("PATH")
            ?.split(File.pathSeparator)
            ?.filter { it.isNotBlank() }
            ?.map { File(it, execName) }
            .orEmpty()

        return (listOfNotNull(javaHomeCandidate) + pathCandidates)
            .filter { it.isFile }
            .distinctBy { runCatching { it.canonicalPath }.getOrDefault(it.absolutePath) }
            .firstOrNull { majorVersionOf(it) == javaVersion }
    }

    /**
     * Runs `<executable> -version` and parses the major version from its output,
     * handling both the legacy `1.8.0_381` scheme and the modern `17.0.2`/`25-ea` one.
     *
     * @return The major version, or null if the executable can't be run or its
     * output doesn't contain a recognizable version string.
     */
    private fun majorVersionOf(executable: File): Int? = runCatching {
        val process = ProcessBuilder(executable.absolutePath, "-version")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()

        val match = Regex("version \"(\\d+)(?:\\.(\\d+))?").find(output) ?: return@runCatching null
        val (major, minor) = match.destructured
        if (major == "1" && minor.isNotEmpty()) minor.toInt() else major.toInt()
    }.getOrNull()

    private fun extractArchive(archive: File, targetDir: File) {
        if (archive.name.endsWith(".zip")) extractZip(archive, targetDir)
        else extractTarGz(archive, targetDir)
    }

    /**
     * Extracts a ZIP archive into [targetDir].
     * The top-level directory included in Adoptium ZIPs is stripped automatically.
     */
    private fun extractZip(archive: File, targetDir: File) {
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val relative = entry.name.substringAfter("/")
                if (relative.isNotBlank()) {
                    val out = File(targetDir, relative)
                    if (entry.isDirectory) out.mkdirs()
                    else { out.parentFile?.mkdirs(); out.outputStream().use { zip.copyTo(it) } }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    /**
     * Extracts a `.tar.gz` archive via the system `tar` command.
     * `--strip-components=1` removes the root JDK directory prefix.
     */
    private fun extractTarGz(archive: File, targetDir: File) {
        ProcessBuilder("tar", "-xzf", archive.absolutePath, "-C", targetDir.absolutePath, "--strip-components=1")
            .inheritIO()
            .start()
            .waitFor()
    }

    private fun detectOs(): String = System.getProperty("os.name").lowercase().let {
        when {
            it.contains("win") -> "windows"
            it.contains("mac") -> "mac"
            else -> "linux"
        }
    }

    private fun detectArch(): String = when (System.getProperty("os.arch").lowercase()) {
        "aarch64" -> "aarch64"
        else -> "x64"
    }

    private fun isWindows(): Boolean = detectOs() == "windows"
}
