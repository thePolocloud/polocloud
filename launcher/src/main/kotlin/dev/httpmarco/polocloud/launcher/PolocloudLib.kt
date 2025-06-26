package dev.httpmarco.polocloud.launcher

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

class PolocloudLib(val name: String) {

    private val path: Path = Paths.get("polocloud-%s-%s.jar".format(name, polocloudVersion()))

    companion object {
        /**
         * Creates a list of PolocloudLib instances from the provided names.
         */
        fun of(vararg name: String): List<PolocloudLib> {
            return Arrays.stream(name).map {
                return@map PolocloudLib(it)
            }.toList()
        }
    }

    /**
     * Returns the name of the Polocloud library.
     */
    fun target(): Path {
        return LIB_DIRECTORY.resolve(path)
    }

    /**
     * Reads the manifest of the Polocloud library and returns the value for the specified key.
     */
    fun mainClass(): String {
        return readManifest("Main-Class", target())!!
    }

    /**
     * Copies the Polocloud library from the classpath to the target directory.
     */
    fun copyFromClasspath() {
        try {
            Files.copy(
                Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResourceAsStream(path.toString())),
                target(),
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (e: IOException) {
            System.err.println("Failed to copy polocloud library from classpath: " + e.message)
            throw RuntimeException(e)
        }
    }
}