package de.polocloud.updater

import de.polocloud.common.version.PolocloudVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile
import java.util.jar.JarInputStream
import kotlin.system.exitProcess

/** Outcome of [Updater.download]. */
sealed interface UpdateResult {
    data class Applied(val version: PolocloudVersion) : UpdateResult
    data object UpToDate : UpdateResult
    data class Failed(val reason: String) : UpdateResult
}

/**
 * Performs an actual self-update, unlike [UpdateChecker] which only ever reports.
 * Only ever runs when explicitly requested: on boot when `general.autoUpdate` is
 * enabled, or via the `update` command.
 *
 * Never touches the launcher jar the JVM is currently running from: for a
 * `java -jar <this>.jar` launch, the JVM keeps that exact file locked for the whole
 * process lifetime, so an in-place overwrite fails on Windows with a sharing
 * violation (it only appears to work on Linux/macOS, where POSIX detaches the inode
 * from the directory entry).
 *
 * Instead, the downloaded release jar is unpacked into [UPDATE_DIR] — every module it
 * embeds under `.cache/dependencies/` (`common`, `node`, `proto`, ... — the same flat
 * layout the runner module's own `Expender` reads at boot) is re-nested by its own
 * manifest's `groupId`/`artifactId`/`version` into the same
 * `groupId/artifactId/version/artifactId-version.jar` layout the runner's
 * `.cache/dependencies` cache already uses, plus a `version` marker recording the new
 * version. Nothing here is locked or in use: it all sits untouched until the runner's
 * `UpdateStaging` (in the `runner` module) applies it at the very start of its *next*
 * boot, before building its classpath — so restarting the very same launcher jar is
 * enough to pick up the new node/common/proto/shared/... jars.
 */
object Updater {

    private val logger: Logger = LoggerFactory.getLogger("Updater")

    /** System property the launcher stores its original process arguments under, so [relaunch] can reuse them. */
    const val LAUNCH_ARGS_PROPERTY = "polocloud.launch.args"

    /** Separator [LAUNCH_ARGS_PROPERTY] is joined with — arbitrary args never contain this control character. */
    const val LAUNCH_ARGS_SEPARATOR = ""

    /** Where staged module jars + the version marker are written — applied by the runner's `UpdateStaging` on its next boot. */
    private val UPDATE_DIR: Path = Paths.get(".cache", "update")

    /** Prefix of the flat module-jar entries a release jar embeds — mirrors `PolocloudParameters.EXPENDER_RUNTIME_CACHE`. */
    private const val EMBEDDED_MODULES_PREFIX = ".cache/dependencies/"

    private const val VERSION_MARKER_FILE = "version"

    /**
     * Downloads the newer release's jar (if any) and unpacks its embedded modules into
     * [UPDATE_DIR]. Never restarts the process — the caller decides whether/when to
     * relaunch so the runner picks the staged update up.
     */
    fun download(
        currentVersion: PolocloudVersion = PolocloudVersion.CURRENT,
        fetcher: ReleaseFetcher = GithubReleaseFetcher(),
    ): UpdateResult {
        val update = runCatching { UpdateChecker.findAvailableUpdate(currentVersion, fetcher.fetchReleases()) }
            .getOrElse { return UpdateResult.Failed(it.message ?: "update check failed") }
            ?: return UpdateResult.UpToDate

        val asset = update.release.assets.firstOrNull { it.name.endsWith(".jar") }
            ?: return UpdateResult.Failed("release ${update.version.toDisplayString()} has no downloadable jar asset")

        return try {
            val releaseJar = downloadToTemp(asset)
            try {
                stage(releaseJar)
            } finally {
                Files.deleteIfExists(releaseJar)
            }
            UpdateResult.Applied(update.version)
        } catch (e: Exception) {
            UpdateResult.Failed(e.message ?: "download failed")
        }
    }

    /**
     * [download]s the update and, if one was staged, relaunches the same jar with the
     * same process arguments as a detached process before exiting this one — its next
     * boot applies the staged update before doing anything else. Called once at boot
     * when `general.autoUpdate` is enabled — blocking is the point: it must finish (and
     * possibly relaunch) before the node starts serving anything. Never throws: any
     * failure is logged and treated as "nothing to do", so it can never brick boot.
     */
    fun downloadAndRestartIfAvailable(
        currentVersion: PolocloudVersion = PolocloudVersion.CURRENT,
        fetcher: ReleaseFetcher = GithubReleaseFetcher(),
    ) {
        when (val result = runCatching { download(currentVersion, fetcher) }.getOrElse { UpdateResult.Failed(it.message ?: "unknown error") }) {
            is UpdateResult.Applied -> {
                val jar = runningJarPath()
                if (jar == null) {
                    logger.warn("Staged update to {}, but could not determine the running jar to restart it — apply it on the next manual restart.", result.version.toDisplayString())
                    return
                }
                logger.info("Staged update to {}, restarting to apply it...", result.version.toDisplayString())
                relaunch(jar)
            }
            is UpdateResult.Failed -> logger.warn("Auto-update skipped: {}", result.reason)
            UpdateResult.UpToDate -> logger.debug("Already up to date, skipping auto-update.")
        }
    }

    private fun runningJarPath(): Path? =
        System.getProperty("java.class.path")
            ?.let { runCatching { Paths.get(it) }.getOrNull() }
            ?.takeIf { Files.isRegularFile(it) }

    private fun downloadToTemp(asset: GithubAsset): Path {
        val tmp = Files.createTempFile("polocloud-update-", ".jar")
        val connection = URI.create(asset.browserDownloadUrl).toURL().openConnection()
        connection.connectTimeout = 10_000
        connection.readTimeout = 30_000
        connection.getInputStream().use { input ->
            Files.copy(input, tmp, StandardCopyOption.REPLACE_EXISTING)
        }
        return tmp
    }

    /** Unpacks every embedded module inside [releaseJar] into [UPDATE_DIR], plus a version marker. */
    private fun stage(releaseJar: Path) {
        Files.createDirectories(UPDATE_DIR)

        JarFile(releaseJar.toFile()).use { jar ->
            val version = jar.manifest?.mainAttributes?.getValue("version")
                ?: error("downloaded release jar has no 'version' manifest attribute")

            val entries = jar.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.isDirectory || !entry.name.startsWith(EMBEDDED_MODULES_PREFIX) || !entry.name.endsWith(".jar")) {
                    continue
                }

                val bytes = jar.getInputStream(entry).use { it.readBytes() }
                val module = readModuleManifest(bytes) ?: continue

                val target = UPDATE_DIR
                    .resolve(Paths.get(module.groupId.replace(".", "/"), module.artifactId, module.version))
                    .resolve("${module.artifactId}-${module.version}.jar")

                Files.createDirectories(target.parent)
                Files.write(target, bytes)
            }

            Files.writeString(UPDATE_DIR.resolve(VERSION_MARKER_FILE), version)
        }
    }

    private data class EmbeddedModule(val groupId: String, val artifactId: String, val version: String)

    private fun readModuleManifest(jarBytes: ByteArray): EmbeddedModule? =
        JarInputStream(jarBytes.inputStream()).use { stream ->
            val attrs = stream.manifest?.mainAttributes ?: return null
            val groupId = attrs.getValue("groupId") ?: return null
            val artifactId = attrs.getValue("artifactId") ?: return null
            val version = attrs.getValue("version") ?: return null
            EmbeddedModule(groupId, artifactId, version)
        }

    /** Spawns a fresh, detached `java -jar <jar> <original args>` process, then exits this one. */
    private fun relaunch(jar: Path): Nothing {
        val javaBin = Paths.get(System.getProperty("java.home"), "bin", "java").toString()
        val launchArgs = System.getProperty(LAUNCH_ARGS_PROPERTY)
            ?.takeIf { it.isNotEmpty() }
            ?.split(LAUNCH_ARGS_SEPARATOR)
            ?: emptyList()

        val command = mutableListOf(javaBin, "-jar", jar.toAbsolutePath().toString())
        command += launchArgs

        ProcessBuilder(command)
            .directory(File(System.getProperty("user.dir")))
            .inheritIO()
            .start()

        exitProcess(0)
    }
}