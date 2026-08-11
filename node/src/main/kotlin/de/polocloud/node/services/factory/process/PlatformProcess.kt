package de.polocloud.node.services.factory.process

import de.polocloud.node.services.factory.platform.Platform
import de.polocloud.node.services.factory.platform.PlatformVersion
import de.polocloud.node.services.factory.platform.PlatformVersionSource
import de.polocloud.node.services.factory.template.resolveJavaVersion
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.nio.file.Paths

/**
 * Manages downloading and launching a specific [PlatformVersion] of a [Platform].
 *
 * @param platform The platform configuration providing language, global arguments,
 *                 and Java version range breakpoints.
 * @param version  The specific version to download and start.
 */
class PlatformProcess(
    private val platform: Platform,
    private val version: PlatformVersion
) {

    private val logger = LoggerFactory.getLogger(PlatformProcess::class.java)
    private val jarName = "${platform.name}-${version.version}-${version.build}.jar"

    private companion object {
        // Services now start concurrently (see ServiceQueue.drainQueue), so two services on
        // the same platform/version can race into cachedJar() at once. Without a lock, both
        // would see the cache miss and write the same tmp/cache file in parallel, corrupting
        // it. One process-wide lock is enough — downloads only happen on a cache miss, which
        // is rare and short-lived compared to the process lifetime.
        private val downloadLock = Any()
    }

    /**
     * Ensures the platform JAR is available inside [targetDir].
     *
     * The JAR is downloaded once into a shared cache ([PlatformJarCache]) and reused across
     * services; on each start it is copied from that cache into [targetDir] (the
     * service work directory, which is wiped on shutdown, so the JAR itself must live
     * there for the process to run in the right directory).
     *
     * @param targetDir Directory the JAR is placed into (the service work directory).
     * @return The JAR [File] inside [targetDir].
     */
    fun download(targetDir: File): File {
        targetDir.mkdirs()
        val target = File(targetDir, jarName)
        if (target.exists()) return target

        val cached = cachedJar()
        cached.copyTo(target, overwrite = true)
        return target
    }

    /**
     * Returns the JAR from the shared cache, downloading it there once if missing.
     * The download goes to a temporary file that is only moved into place on success,
     * so an interrupted download never leaves a corrupt JAR in the cache.
     */
    private fun cachedJar(): File = synchronized(downloadLock) {
        PlatformJarCache.DIRECTORY.mkdirs()
        val cached = PlatformJarCache.fileFor(platform.name, version.version, version.build)
        if (cached.exists()) {
            logger.debug("$jarName already cached")
            return@synchronized cached
        }

        if (version.source == PlatformVersionSource.LOCAL_FILE) {
            // Custom local-file versions are copied into this cache the moment they're
            // attached (see CustomPlatformService.addVersion), specifically so a live jar
            // never depends on the operator's original file surviving until a service is
            // actually started. Reaching this branch means that cache entry was lost after
            // the fact (e.g. someone cleared .cache/platforms/versions by hand) — there is
            // no original download URL to fall back to, so fail clearly instead of the
            // confusing URISyntaxException an empty downloadUrl would otherwise produce.
            error(
                "Cached jar for custom platform '${platform.name}' version '${version.version}' " +
                    "is missing and was sourced from a local file, so it cannot be re-downloaded " +
                    "automatically. Re-attach the version via 'platform version add'."
            )
        }

        logger.info("Downloading $jarName ...")
        val tmp = File(PlatformJarCache.DIRECTORY, "$jarName.tmp")
        URI(version.downloadUrl).toURL().openStream().use { input ->
            tmp.outputStream().use { output -> input.copyTo(output) }
        }
        tmp.copyTo(cached, overwrite = true)
        tmp.delete()
        logger.info("done (${cached.length() / 1024} KB)")
        cached
    }

    /**
     * Starts the platform process from the given [jarFile].
     *
     * The required Java version is resolved from [Platform.javaVersionRanges].
     * If a range matches, [JavaRuntimeManager] downloads the appropriate JRE
     * automatically. Falls back to the system `java` when no range is configured.
     *
     * Any entries in [environment] are added to the child process environment —
     * this is how the provisioned mTLS identity directory is handed to the
     * service via `POLOCLOUD_IDENTITY_DIR`.
     *
     * @param jarFile The JAR file to execute.
     * @param environment Extra environment variables to expose to the process.
     * @return The running [Process].
     * @throws IllegalStateException if no runtime is registered for the platform language.
     */
    fun start(jarFile: File, environment: Map<String, String> = emptyMap()): Process {
        val executable = resolveExecutable()
        val runtime = PlatformRuntime.forLanguage(platform.language)
        val command = runtime.buildCommand(executable, jarFile, platform.jvmArgs, platform.globalArgs)
        logger.info("Starting ${platform.name} ${version.version} (build ${version.build})")
        return ProcessBuilder(command)
            .directory(jarFile.parentFile)
            .redirectErrorStream(true)
            .apply { environment().putAll(environment) }
            .start()
    }

    /**
     * Resolves the java executable path for this version.
     * Downloads the matching JRE via [JavaRuntimeManager] if [Platform.javaVersionRanges]
     * contains a matching breakpoint; otherwise returns `"java"` (system default).
     */
    private fun currentJavaExecutable(): String {
        val executable = if (System.getProperty("os.name").startsWith("Windows")) {
            "java.exe"
        } else {
            "java"
        }

        return Paths.get(System.getProperty("java.home"), "bin", executable).toString()
    }

    private fun resolveExecutable(): String {
        val requiredJava = resolveJavaVersion(version.version, platform.javaVersionRanges)
        return requiredJava?.let(JavaRuntimeManager::ensure)?.absolutePath
            ?: currentJavaExecutable()
    }
}
