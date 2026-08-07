package de.polocloud.node.services.factory.template

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.URI
import java.util.zip.ZipInputStream

/**
 * Fetches the platform templates from the latest GitHub release of
 * [thePolocloud/polocloud-platforms](https://github.com/thePolocloud/polocloud-platforms)
 * and extracts them into the local platform cache.
 *
 * The release is expected to contain an asset named [ASSET_NAME] which bundles the
 * platform template JSON files consumed by [loadTemplatesFromCache].
 */
object PlatformDownloader {

    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/thePolocloud/polocloud-platforms/releases/latest"

    private const val ASSET_NAME = "polocloud-platforms.zip"

    /**
     * Downloads [ASSET_NAME] from the latest release and unpacks it into [cacheDir].
     *
     * @param cacheDir Target directory for the extracted template files.
     * @throws IllegalStateException if the release does not expose the expected asset.
     */
    fun downloadInto(cacheDir: File) {
        val downloadUrl = resolveAssetUrl()
        cacheDir.mkdirs()

        val archive = File(cacheDir, ASSET_NAME)
        URI(downloadUrl).toURL().openStream().use { input ->
            archive.outputStream().use { input.copyTo(it) }
        }

        extractZip(archive, cacheDir)
        archive.delete()
    }

    /**
     * Queries the GitHub API for the latest release and resolves the download URL
     * of the [ASSET_NAME] asset.
     */
    private fun resolveAssetUrl(): String {
        val root = Json.parseToJsonElement(URI(LATEST_RELEASE_URL).toURL().readText()).jsonObject
        // Safe-navigate every step instead of chained `!!`: an unexpected GitHub API
        // response (shape change, rate-limit error body instead of a release) should fail
        // with a message that says what's missing, not a bare NPE pointing at this line.
        val assets = root["assets"]?.jsonArray
            ?: error("Unexpected GitHub releases API response: missing 'assets' field")
        val asset = assets
            .map { it.jsonObject }
            .firstOrNull { it["name"]?.jsonPrimitive?.content == ASSET_NAME }
            ?: error("Latest release of polocloud-platforms does not contain asset '$ASSET_NAME'")
        return asset["browser_download_url"]?.jsonPrimitive?.content
            ?: error("Unexpected GitHub releases API response: asset '$ASSET_NAME' has no 'browser_download_url' field")
    }

    /**
     * Extracts a ZIP archive into [targetDir], creating parent directories as needed.
     */
    private fun extractZip(archive: File, targetDir: File) {
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.isNotBlank()) {
                    val out = File(targetDir, entry.name)
                    if (entry.isDirectory) out.mkdirs()
                    else { out.parentFile?.mkdirs(); out.outputStream().use { zip.copyTo(it) } }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
}
