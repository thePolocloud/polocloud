package de.polocloud.common.communication.certificate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

class FilePermissionsTest {

    private fun isPosix(): Boolean =
        FileSystems.getDefault().supportedFileAttributeViews().contains("posix")

    @Test
    fun `restrictToOwnerOnly chmods a file to rw-------`(@TempDir dir: File) {
        assumeTrue(isPosix(), "POSIX permissions not supported on this platform")

        val file = File(dir, "private-key.pem").apply { writeText("secret") }

        restrictToOwnerOnly(file)

        val perms = Files.getPosixFilePermissions(file.toPath())
        assertEquals(PosixFilePermissions.fromString("rw-------"), perms)
    }

    @Test
    fun `restrictDirToOwnerOnly chmods a directory to rwx------`(@TempDir dir: File) {
        assumeTrue(isPosix(), "POSIX permissions not supported on this platform")

        val sub = File(dir, "identity").apply { mkdirs() }

        restrictDirToOwnerOnly(sub.toPath())

        val perms = Files.getPosixFilePermissions(sub.toPath())
        assertEquals(PosixFilePermissions.fromString("rwx------"), perms)
    }

    @Test
    fun `restrictToOwnerOnly never throws, even for a missing file`() {
        restrictToOwnerOnly(File("/does/not/exist/private-key.pem"))
    }
}
