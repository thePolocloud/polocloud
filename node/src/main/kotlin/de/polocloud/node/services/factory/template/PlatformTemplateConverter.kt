package de.polocloud.node.services.factory.template

import de.polocloud.node.services.factory.platform.Platform
import de.polocloud.node.services.factory.platform.PlatformVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI

// Every version-detection request goes through this dispatcher. Resolving a platform's
// versions used to mean one blocking HTTP round trip per version, run one after another —
// with a few dozen versions per platform across several platforms, that serialized into
// several seconds of dead time on every node start. Bounding (rather than uncapping)
// concurrency keeps the fan-out from looking like a burst to the remote API.
private val VERSION_FETCH_DISPATCHER = Dispatchers.IO.limitedParallelism(8)

/**
 * Converts a list of [PlatformTemplate]s into fully resolved [Platform]s by
 * fetching available versions from the remote API defined in each template.
 *
 * Platforms, and every version within a platform, are resolved concurrently rather than
 * one HTTP call at a time.
 *
 * @param items Templates loaded from the cache directory.
 * @return Platforms with their [PlatformVersion] lists populated.
 */
fun convertTemplatesToPlatform(items: List<PlatformTemplate>): List<Platform> = runBlocking {
    items.map { template ->
        async {
            Platform(
                name = template.name,
                type = template.type,
                language = template.language,
                jvmArgs = template.jvmArgs,
                globalArgs = template.globalArgs,
                tasks = template.tasks,
                javaVersionRanges = template.javaVersionRanges,
                versions = scanVersions(template)
            )
        }
    }.awaitAll()
}

/**
 * Resolves all available [PlatformVersion]s for the given [template], according to its
 * [VersionDetection.mode]:
 * - `AUTOMATIC` fetches and parses versions/builds from the remote API described by
 *   [VersionDetection.baseUrl]/[VersionDetection.parse]. Returns an empty list if any request fails.
 * - `STATIC` returns a single fixed [PlatformVersion] built from [VersionDetection.version]/
 *   [VersionDetection.downloadUrl], with no build number of its own.
 *
 * Any other mode yields an empty list.
 */
private suspend fun scanVersions(template: PlatformTemplate): List<PlatformVersion> {
    val detection = template.versionDetection
    return when (detection.mode) {
        "AUTOMATIC" -> scanAutomaticVersions(detection)
        "STATIC" -> listOf(PlatformVersion(detection.version, STATIC_PLATFORM_BUILD, detection.downloadUrl))
        else -> emptyList()
    }
}

/**
 * Build number attached to every `STATIC` platform version. Such a platform has no queryable
 * build concept — its single version is a fixed download URL — so this is a shared placeholder,
 * matching the convention used for custom platforms (see
 * [de.polocloud.node.services.factory.platform.custom.CUSTOM_PLATFORM_BUILD]).
 */
private const val STATIC_PLATFORM_BUILD = 0

private suspend fun scanAutomaticVersions(detection: VersionDetection): List<PlatformVersion> {
    val parse = detection.parse ?: return emptyList()
    return runCatching {
        val rootJson = fetchJson(detection.baseUrl)
        val versions = resolveAllVersions(rootJson, parse.versionPath)
        coroutineScope {
            versions.map { version -> async { fetchVersionDetails(detection, parse, version) } }.awaitAll()
        }.filterNotNull()
    }.getOrDefault(emptyList())
}

/**
 * Fetches the latest build details for a single [version] using the given [detection]/[parse] config.
 *
 * The download URL is resolved either via [VersionParse.downloadPath] (direct JSON path)
 * or by substituting placeholders in [VersionParse.downloadUrl].
 *
 * @return A [PlatformVersion] for the given version, or null if resolution fails.
 */
private suspend fun fetchVersionDetails(detection: VersionDetection, parse: VersionParse, version: String): PlatformVersion? {
    return runCatching {
        val buildUrl = parse.buildUrl
            .replace("{baseUrl}", detection.baseUrl)
            .replace("{version}", version)
        val buildJson = fetchJson(buildUrl)
        val build = resolveElement(buildJson, parse.buildPath)
            ?.jsonPrimitive?.int ?: return null
        val downloadUrl = if (parse.downloadPath != null) {
            resolveElement(buildJson, parse.downloadPath)
                ?.jsonPrimitive?.content ?: return null
        } else {
            parse.downloadUrl
                .replace("{baseUrl}", detection.baseUrl)
                .replace("{version}", version)
                .replace("{build}", build.toString())
        }
        PlatformVersion(version, build, downloadUrl)
    }.getOrNull()
}

/**
 * Fetches the content of [url] and parses it as a [JsonElement], off-loaded to
 * [VERSION_FETCH_DISPATCHER] since [java.net.URI.toURL] / [java.net.URL.readText] block
 * the calling thread.
 *
 * @throws Exception if the HTTP request fails or the response is not valid JSON.
 */
private suspend fun fetchJson(url: String): JsonElement = withContext(VERSION_FETCH_DISPATCHER) {
    val text = URI(url).toURL().readText()
    Json.parseToJsonElement(text)
}
