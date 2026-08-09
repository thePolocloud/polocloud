package de.polocloud.node.services.factory.process

import java.io.File

/**
 * [PlatformRuntime] for Go-based platforms, which ship as a single self-contained
 * binary rather than a JAR run through a JVM. [executable] is unused — the binary
 * ([jarFile]) is invoked directly.
 *
 * Resulting command structure:
 * ```
 * <binary> [jvmArgs] [args]
 * ```
 */
object GoRuntime : PlatformRuntime {

    override val language = "GO"

    override fun buildCommand(executable: String, jarFile: File, jvmArgs: List<String>, args: List<String>): List<String> {
        if (!isWindows()) jarFile.setExecutable(true)
        return listOf(jarFile.absolutePath) + jvmArgs + args
    }

    private fun isWindows(): Boolean = System.getProperty("os.name").startsWith("Windows")
}
