package dev.httpmarco.polocloud.common.dependency.checksum

import java.io.File
import java.security.MessageDigest

/**
 * Utility object for computing checksums of files.
 */
object FileChecksum {

    private const val BUFFER_SIZE = 1024

    /**
     * Computes the SHA-1 checksum of this file.
     *
     * @receiver the file to compute the checksum for
     * @return the SHA-1 checksum as a lowercase hexadecimal string
     */
    fun File.sha1(): String = checksum("SHA-1")

    /**
     * Computes the SHA-256 checksum of this file.
     *
     * @receiver the file to compute the checksum for
     * @return the SHA-256 checksum as a lowercase hexadecimal string
     */
    fun File.sha256(): String = checksum("SHA-256")

    /**
     * Computes a checksum for this file using the given algorithm.
     */
    private fun File.checksum(algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        inputStream().use { fis ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
