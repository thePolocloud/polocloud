package de.polocloud.node.services.factory.platform.custom

import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.zip.ZipFile

/**
 * Validates a custom platform version's source before it is accepted — the "secure checks"
 * an operator's URL/local-file input goes through in [CustomPlatformService.addVersion], so a
 * typo'd URL or a wrong/missing file path is rejected up front instead of surfacing much later
 * as a confusing failure when a service actually tries to start.
 *
 * Both functions return `null` on success, or a human-readable rejection reason.
 */
object PlatformSourceValidator {

    private const val TIMEOUT_MS = 5000

    /**
     * Confirms [url] uses http(s) and actually resolves, using a `HEAD` request (falling back
     * to `GET` since some hosts reject `HEAD` with 405/501 — a range-free `GET` still proves
     * reachability without pulling the whole body twice for a jar that may be large).
     */
    fun verifyUrl(url: String): String? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return "'$url' is not a valid URL."
        if (uri.scheme?.lowercase() !in setOf("http", "https")) {
            return "URL must use http or https: '$url'"
        }

        return runCatching {
            val code = probe(uri, "HEAD")
            val effectiveCode = if (code == 405 || code == 501) probe(uri, "GET") else code
            if (effectiveCode in 200..399) null else "URL responded with HTTP $effectiveCode: '$url'"
        }.getOrElse { "Could not reach '$url': ${it.message}" }
    }

    private fun probe(uri: URI, method: String): Int {
        val connection = uri.toURL().openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.responseCode
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Confirms [path] points at a readable file on this node's filesystem.
     *
     * When [requireJar] is set (the default, used for JAVA-language platforms), [path] must
     * additionally be a well-formed jar: opened as a [ZipFile] rather than only checking the
     * extension, so a corrupt or non-jar file is caught here instead of at service start.
     * Other languages (e.g. GO) ship a plain executable binary instead, so that check is skipped.
     */
    fun verifyLocalFile(path: String, requireJar: Boolean = true): String? {
        val file = File(path)
        if (!file.exists()) return "File does not exist: '$path'"
        if (!file.isFile) return "Not a file: '$path'"
        if (!file.canRead()) return "File is not readable: '$path'"
        if (!requireJar) return null
        if (!file.name.endsWith(".jar", ignoreCase = true)) return "File must be a .jar file: '$path'"

        return runCatching {
            ZipFile(file).use { }
            null
        }.getOrElse { "File is not a valid jar archive: '$path' (${it.message})" }
    }
}
