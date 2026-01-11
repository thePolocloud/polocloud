package dev.httpmarco.polocloud.common.dependency.scanning

import dev.httpmarco.polocloud.common.dependency.Dependency
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.jar.JarFile

private const val BLOB_FILE = "dependencies.blob"

class OwnBlobScanner(private val file: File) : DependencyScanner<String> {

    override fun scanDependencies(): List<String> {
        if (!file.exists()) {
            error("Jar not exists: ${file.absolutePath}")
        }

        JarFile(file).use { jar ->
            val entry = jar.getJarEntry(BLOB_FILE)
                ?: error("dependencies.blob not found in JAR: ${file.absolutePath}")

            jar.getInputStream(entry).use { stream ->
                val content = stream
                    .bufferedReader(StandardCharsets.UTF_8)
                    .readText()

                return content
                    .lines()
                    .filter { it.isNotBlank() }
            }
        }
    }

    override fun mapToDependency(dependency: String): Dependency {
        val parts = dependency.split(";")

        return Dependency(
            groupId = parts[0],
            artifactId = parts[1],
            version = parts[2],
            url = parts[3],
            checksum = parts[4]
        )
    }
}
