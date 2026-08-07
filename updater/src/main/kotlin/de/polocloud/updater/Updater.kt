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
import java.security.MessageDigest
import java.time.Duration
import java.util.concurrent.TimeUnit
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

    /** How long [relaunch] waits for the freshly spawned process to still be alive before trusting it and exiting. */
    private val RELAUNCH_HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(5)

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
        val checksumAsset = update.release.assets.firstOrNull { it.name == "${asset.name}.sha256" }

        return try {
            val releaseJar = downloadToTemp(asset, checksumAsset)
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
                if (!relaunch(jar)) {
                    logger.error(
                        "New process failed its post-relaunch health check — aborting the update and continuing " +
                            "to run the current (still-working) version. The staged update remains in {} and will " +
                            "be retried on the next auto-update attempt.",
                        UPDATE_DIR,
                    )
                }
            }
            is UpdateResult.Failed -> logger.warn("Auto-update skipped: {}", result.reason)
            UpdateResult.UpToDate -> logger.debug("Already up to date, skipping auto-update.")
        }
    }

    /**
     * Restarts the current process (same jar, same original launch args) with
     * [extraJvmArgs] inserted as additional `-D...` JVM arguments before `-jar` — e.g. so
     * the node's `cluster join` wizard can hand a one-shot join token to the next boot via
     * system properties, the same way [downloadAndRestartIfAvailable] restarts to apply a
     * staged update, just parameterized instead of always relaunching with none.
     *
     * @return `false` if the running jar path couldn't be determined — nothing was
     * restarted, and the caller should fall back to telling the operator to restart
     * manually. Otherwise this function never returns (the process exits).
     */
    fun restart(extraJvmArgs: List<String> = emptyList()): Boolean {
        val jar = runningJarPath() ?: return false
        return relaunch(jar, extraJvmArgs)
    }

    private fun runningJarPath(): Path? =
        System.getProperty("java.class.path")
            ?.let { runCatching { Paths.get(it) }.getOrNull() }
            ?.takeIf { Files.isRegularFile(it) }

    /**
     * Downloads [asset] to a temp file and, if [checksumAsset] is present (the release
     * workflow publishes a `<jar-name>.sha256` sibling asset alongside every jar — see
     * `master-prerelease.yml`'s "Stage release assets" step), verifies its SHA-256 digest
     * against it before returning, so a corrupted download or a tampered asset is caught
     * here rather than staged and later launched. Throws (and leaves nothing staged) on a
     * mismatch, or if an older release has no checksum asset published for it at all.
     */
    private fun downloadToTemp(asset: GithubAsset, checksumAsset: GithubAsset?): Path {
        val tmp = Files.createTempFile("polocloud-update-", ".jar")
        try {
            val connection = URI.create(asset.browserDownloadUrl).toURL().openConnection()
            connection.connectTimeout = 10_000
            connection.readTimeout = 30_000
            connection.getInputStream().use { input ->
                Files.copy(input, tmp, StandardCopyOption.REPLACE_EXISTING)
            }

            if (checksumAsset == null) {
                logger.warn(
                    "Release asset '{}' has no accompanying '.sha256' checksum asset — skipping integrity " +
                        "verification. This should only happen for releases published before checksum publishing " +
                        "was introduced.",
                    asset.name,
                )
            } else {
                val expected = fetchChecksum(checksumAsset)
                val actual = sha256Hex(tmp)
                check(actual.equals(expected, ignoreCase = true)) {
                    "SHA-256 mismatch for downloaded release asset '${asset.name}': expected $expected, got $actual " +
                        "— refusing to stage a possibly corrupted or tampered download"
                }
            }

            return tmp
        } catch (e: Exception) {
            Files.deleteIfExists(tmp)
            throw e
        }
    }

    /** Downloads a small `<jar-name>.sha256` asset and returns its hex digest (its only content). */
    private fun fetchChecksum(asset: GithubAsset): String {
        val connection = URI.create(asset.browserDownloadUrl).toURL().openConnection()
        connection.connectTimeout = 10_000
        connection.readTimeout = 30_000
        val text = connection.getInputStream().use { it.readBytes().toString(Charsets.UTF_8) }
        // Tolerate the coreutils `sha256sum <file>` format ("<hex>  <filename>") too, in
        // case the checksum file is ever regenerated by hand instead of by the workflow.
        return text.trim().substringBefore(' ').substringBefore('\t')
    }

    private fun sha256Hex(file: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
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

    /**
     * Spawns a fresh, detached `java <extraJvmArgs> -jar <jar> <original args>` process,
     * then waits up to [RELAUNCH_HEALTH_CHECK_TIMEOUT] to confirm it's still alive before
     * exiting this one.
     *
     * There's no readiness signal (PID file, health port, ...) shared between the old and
     * new process to check instead, so "still running after the check window" is the most
     * meaningful thing observable from here — good enough to catch the common failure mode
     * of a corrupt/incompatible jar dying on startup (missing main class, `NoClassDefFoundError`,
     * ...) before this process, the last known-good one, is given up. It will not catch a jar
     * that starts fine but fails later — this is a startup sanity check, not a rollback
     * mechanism.
     *
     * @return `true` if the health check passed — in which case this process is about to
     * exit and the function never actually returns to its caller. `false` if the new process
     * exited within the check window; in that case this process keeps running and nothing
     * was exited.
     */
    private fun relaunch(jar: Path, extraJvmArgs: List<String> = emptyList()): Boolean {
        val javaBin = Paths.get(System.getProperty("java.home"), "bin", "java").toString()
        val launchArgs = System.getProperty(LAUNCH_ARGS_PROPERTY)
            ?.takeIf { it.isNotEmpty() }
            ?.split(LAUNCH_ARGS_SEPARATOR)
            ?: emptyList()

        val command = mutableListOf(javaBin)
        command += extraJvmArgs
        command += listOf("-jar", jar.toAbsolutePath().toString())
        command += launchArgs

        val process = ProcessBuilder(command)
            .directory(File(System.getProperty("user.dir")))
            .inheritIO()
            .start()

        val exitedAlready = process.waitFor(RELAUNCH_HEALTH_CHECK_TIMEOUT.seconds, TimeUnit.SECONDS)
        if (!exitedAlready) {
            // Still running after the check window - trust it and hand off.
            exitProcess(0)
        }

        logger.error(
            "New process (pid {}) exited with code {} within {}s of starting.",
            process.pid(), process.exitValue(), RELAUNCH_HEALTH_CHECK_TIMEOUT.seconds,
        )
        return false
    }
}