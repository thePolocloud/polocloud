package dev.httpmarco.polocloud.common.dependency.checksum

import java.io.File
import java.security.MessageDigest

/**
 * Utility object for computing checksums of files.
 */
object FileChecksum {

    /**
     * Computes the SHA-1 checksum of this file.
     *
     * This function reads the file in blocks of 1024 bytes to avoid loading
     * the entire file into memory at once, making it suitable for large files.
     *
     * @receiver the file to compute the checksum for
     * @return the SHA-1 checksum as a lowercase hexadecimal string
     * @throws java.security.NoSuchAlgorithmException if SHA-1 is not available
     * @throws java.io.IOException if an I/O error occurs while reading the file
     */
    fun File.sha1(): String {
        val digest = MessageDigest.getInstance("SHA-1")
        this.inputStream().use { fis ->
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
