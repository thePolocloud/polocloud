package de.polocloud.node.services.factory.template

import de.polocloud.node.services.factory.platform.JavaVersionRange
import kotlinx.serialization.Serializable

/**
 * Represents a platform configuration loaded from a cache JSON file.
 *
 * @param name            Unique platform identifier (e.g. "paper", "velocity").
 * @param type            Platform role: SERVER or PROXY.
 * @param language        Runtime language used to launch the platform (e.g. JAVA).
 * @param jvmArgs         JVM arguments placed before `-jar` (e.g. "-Dcom.mojang.eula.agree=true").
 * @param globalArgs      JVM and program arguments passed when starting a process.
 * @param tasks               Optional tasks applied to specific version ranges.
 * @param javaVersionRanges   Ordered breakpoints mapping platform version ranges to required Java versions.
 * @param versionDetection    Configuration describing how versions are fetched from a remote API.
 */
@Serializable
data class PlatformTemplate(
    val name: String,
    val type: String,
    val language: String,
    val jvmArgs: List<String> = emptyList(),
    val globalArgs: List<String> = emptyList(),
    val tasks: List<ServiceTask> = emptyList(),
    val javaVersionRanges: List<JavaVersionRange> = emptyList(),
    val versionDetection: VersionDetection
)

/**
 * References a reusable task definition that should be applied to a platform,
 * constrained to a range of platform versions.
 *
 * The actual steps are not stored here — they live in a separate task definition
 * file (under `tasks/` in the platform cache) identified by [key]. This keeps a
 * task ("set up server.properties") reusable across multiple platforms.
 *
 * @param key   Identifier of the task definition to apply (e.g. "server_properties").
 * @param from  Lowest platform version (inclusive) the task applies to, or null for no lower bound.
 * @param until Highest platform version (inclusive) the task applies to, or null for no upper bound.
 */
@Serializable
data class ServiceTask(
    val key: String,
    val from: String? = null,
    val until: String? = null
) {

    /**
     * Returns true if this task applies to the given platform [version], i.e. the
     * version falls within the inclusive `[from, until]` range. Missing bounds are
     * treated as open.
     */
    fun appliesTo(version: String): Boolean {
        if (from != null && compareVersions(version, from) < 0) return false
        if (until != null && compareVersions(version, until) > 0) return false
        return true
    }
}

/**
 * Describes how versions are detected for a platform.
 *
 * @param mode        Detection strategy: `AUTOMATIC` resolves versions/builds from a remote
 *                     API via [parse]; `STATIC` skips detection entirely and exposes a single
 *                     fixed [version] downloaded from [downloadUrl] — for platforms whose
 *                     upstream only ever offers a "latest" link with no queryable version/build
 *                     metadata (e.g. a redirect-only download page).
 * @param baseUrl     Root API URL used as the starting point for all requests. Only used when
 *                     [mode] is `AUTOMATIC`.
 * @param parse       Rules defining how to extract versions and build artifacts from the API.
 *                     Only used when [mode] is `AUTOMATIC`.
 * @param version     Fixed version string exposed for this platform. Only used when [mode] is
 *                     `STATIC`.
 * @param downloadUrl Fixed direct download URL for [version]. Only used when [mode] is `STATIC`.
 */
@Serializable
data class VersionDetection(
    val mode: String,
    val baseUrl: String = "",
    val parse: VersionParse? = null,
    val version: String = "",
    val downloadUrl: String = ""
)

/**
 * Defines the JSON parsing rules used to locate versions and builds from a remote API.
 *
 * @param type          Response format (currently only JSON is supported).
 * @param versionPath   JSON path to the version list in the base URL response.
 * @param buildUrl      URL template for fetching builds of a specific version.
 * @param buildPath     JSON path to the build number within the build response.
 * @param downloadUrl   URL template for constructing the download link (used when [downloadPath] is null).
 * @param downloadPath  JSON path to the download URL directly within the build response.
 *                      When set, [downloadUrl] is ignored.
 */
@Serializable
data class VersionParse(
    val type: String,
    val versionPath: String,
    val buildUrl: String,
    val buildPath: String,
    val downloadUrl: String = "",
    val downloadPath: String? = null
)
