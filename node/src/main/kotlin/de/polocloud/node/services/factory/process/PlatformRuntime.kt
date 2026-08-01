package de.polocloud.service.factory.process

import de.polocloud.node.services.factory.process.GoRuntime
import de.polocloud.node.services.factory.process.JavaRuntime
import java.io.File

/**
 * Defines how a specific programming language executes a platform JAR.
 *
 * Each implementation is responsible for constructing the full launch command
 * from the given executable path, artifact, and arguments. Runtimes are
 * registered by language identifier and looked up via [PlatformRuntime.forLanguage].
 */
interface PlatformRuntime {

    /** Language identifier matching [de.polocloud.node.services.factory.platform.Platform.language]. */
    val language: String

    /**
     * Builds the full command list used to launch [jarFile].
     *
     * @param executable Absolute path or name of the runtime executable (e.g. "java" or "/runtimes/java-21/bin/java").
     * @param jarFile    The artifact to execute.
     * @param jvmArgs    JVM arguments placed before `-jar`, sourced from the platform configuration.
     * @param args       Global arguments sourced from the platform configuration.
     * @return Ordered list of command tokens passed to [ProcessBuilder].
     */
    fun buildCommand(executable: String, jarFile: File, jvmArgs: List<String>, args: List<String>): List<String>

    companion object {

        private val registry: MutableMap<String, PlatformRuntime> = mutableMapOf(
            JavaRuntime.language to JavaRuntime,
            GoRuntime.language to GoRuntime
        )

        /**
         * Returns the [PlatformRuntime] registered for [language].
         *
         * @throws IllegalStateException if no runtime is registered for the given language.
         */
        fun forLanguage(language: String): PlatformRuntime =
            registry[language] ?: error("No runtime registered for language: $language")

        /**
         * Registers a custom [PlatformRuntime], replacing any existing entry for the same language.
         */
        fun register(runtime: PlatformRuntime) {
            registry[runtime.language] = runtime
        }
    }
}
