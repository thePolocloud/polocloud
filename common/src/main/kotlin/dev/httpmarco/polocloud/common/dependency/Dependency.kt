package dev.httpmarco.polocloud.common.dependency

import dev.httpmarco.polocloud.common.dependency.checksum.FileChecksum.sha1
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

/**
 * Represents a downloadable dependency (e.g., a JAR) with its Maven coordinates and checksum.
 *
 * @property groupId the group ID of the dependency (e.g., "org.example")
 * @property artifactId the artifact ID of the dependency (e.g., "example-lib")
 * @property version the version of the dependency (e.g., "1.0.0")
 * @property url the URL from which the dependency can be downloaded
 * @property checksum the expected SHA-1 checksum of the dependency file
 */
data class Dependency(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val url: String,
    val checksum: String
) {

    /**
     * Downloads the dependency JAR to the local cache if it does not exist or if the checksum is invalid.
     *
     * The file is stored in `.cache/<groupId>/<artifactId>/<version>/<artifactId>-<version>.jar`.
     * If the download succeeds, the checksum is verified before moving it to the final location.
     *
     * @throws IllegalStateException if the checksum verification fails after download
     */
    fun download() {
        val target = Path.of(".cache")
            .resolve(convertedPathGroupId())
            .resolve(artifactId)
            .resolve(version)
            .resolve(fileName())

        if (target.exists()) {
            if (verifyChecksum(target)) return
            target.deleteIfExists()
        }

        Files.createDirectories(target.parent)

        val tempFile = Files.createTempFile(target.parent, "$artifactId-$version", ".tmp")

        URL(url).openStream().use { input ->
            Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING)
        }

        // Verify checksum after download
        if (!verifyChecksum(tempFile)) {
            tempFile.deleteIfExists()
            error("Checksum verification failed for $artifactId:$version")
        }

        Files.move(
            tempFile,
            target,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE
        )
    }

    /**
     * Verifies the SHA-1 checksum of the given file.
     *
     * @param filePath the path to the file to verify
     * @return true if the file's SHA-1 checksum matches the expected checksum, false otherwise
     */
    private fun verifyChecksum(filePath: Path): Boolean =
        filePath.toFile().sha1().equals(checksum, ignoreCase = true)

    /**
     * Returns the default filename for the dependency JAR.
     *
     * @return the filename in the format `<artifactId>-<version>.jar`
     */
    private fun fileName(): String = "$artifactId-$version.jar"

    /**
     * Converts the groupId to a path-friendly format by replacing '.' with '/'.
     *
     * @return the converted groupId path
     */
    private fun convertedPathGroupId(): String = groupId.replace('.', '/')
}
