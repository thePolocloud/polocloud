package de.polocloud.node.module

import de.polocloud.common.dependency.Dependency
import java.io.File
import java.net.URI

/**
 * Resolves a module's `dependencies:` entries (plain `group:artifact:version` coordinates)
 * against Maven Central. Unlike the node's own `polocloudRuntime`/`dependencies.index`
 * mechanism (build-time resolved, transitive, checksum baked in at build time), this is a
 * runtime lookup with no transitive resolution: exactly one jar is downloaded per
 * declared coordinate. Its checksum is fetched alongside it (the `.sha1`/`.sha256`
 * sidecar every Maven Central artifact publishes) so [Dependency.download] can still
 * verify integrity, even though the module author never had to compute or paste one in.
 */
object ModuleDependencyResolver {

    private const val MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2"

    /** Downloads (if not already cached) and returns the local jar for [coordinate]. */
    fun resolve(coordinate: String): File {
        val parts = coordinate.split(":")
        require(parts.size == 3) { "invalid dependency coordinate '$coordinate', expected group:artifact:version" }
        val (groupId, artifactId, version) = parts

        val groupPath = groupId.replace('.', '/')
        val baseUrl = "$MAVEN_CENTRAL/$groupPath/$artifactId/$version"
        val jarUrl = "$baseUrl/$artifactId-$version.jar"

        val checksum = fetchChecksum("$jarUrl.sha1") ?: fetchChecksum("$jarUrl.sha256")
            ?: throw IllegalStateException("could not fetch a checksum for '$coordinate' from Maven Central")

        val dependency = Dependency(groupId, artifactId, version, jarUrl, checksum)
        dependency.download()
        return dependency.localPath().toFile()
    }

    private fun fetchChecksum(url: String): String? = runCatching {
        URI(url).toURL().openStream().use { it.readBytes() }
            .toString(Charsets.UTF_8)
            .trim()
            .substringBefore(' ') // some repos publish "<hash>  <filename>" instead of just "<hash>"
            .takeIf { it.isNotBlank() }
    }.getOrNull()
}
