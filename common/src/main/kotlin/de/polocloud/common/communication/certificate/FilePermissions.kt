package de.polocloud.common.communication.certificate

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

/**
 * Restricts [file] to owner-only read/write (`rw-------`, chmod 600) on POSIX
 * filesystems. A private key left at the default (typically world-readable) permissions
 * lets any other local user or process on a shared host read it straight off disk —
 * applied to every private key file this project writes (node identity, cluster CA,
 * per-service identity; see [de.polocloud.common.communication.certificate.CertificateStorage]).
 *
 * Best-effort and silent: a no-op on filesystems without POSIX permission support (e.g.
 * Windows), since there's no equivalent single-call restriction there worth the added
 * complexity for what's still typically a single-user host in practice.
 */
fun restrictToOwnerOnly(file: File) {
    runCatching {
        Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-------"))
    }
}

/** Directory equivalent of [restrictToOwnerOnly] (`rwx------`, chmod 700). */
fun restrictDirToOwnerOnly(dir: Path) {
    runCatching {
        Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwx------"))
    }
}
