package dev.httpmarco.polocloud.launcher

import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.Path

/**
 * General class for all parameters
 * For type-safe use
 */

val LIB_DIRECTORY = Path("local/libs")
val REQUIRED_LIBS = arrayOf("agent", "platforms")
const val VERSION_ENV_ID = "polocloud-version"
const val BOOT_LIB = "agent"

/**
 * Returns the version of the Polocloud instance.
 * This is read from the system property `polocloud-version`.
 * If the property is not set, it returns null.
 *
 * @return The version of the Polocloud instance or null if not set.
 */
fun polocloudVersion(): String? {
    return System.getProperty(VERSION_ENV_ID)
}

/**
 * Reads a specific key from the manifest of a JAR file located at the given path.
 *
 * @param key The key to read from the manifest.
 * @param path The path to the JAR file.
 * @return The value associated with the key in the manifest, or null if not found or an error occurs.
 */
fun readManifest(key: String?, path: Path): String? {
    try {
        JarFile(path.toString()).use { jarFile ->
            return jarFile.manifest.mainAttributes.getValue(key)
        }
    } catch (exception: Exception) {
        exception.printStackTrace(System.err)
    }
    return null
}