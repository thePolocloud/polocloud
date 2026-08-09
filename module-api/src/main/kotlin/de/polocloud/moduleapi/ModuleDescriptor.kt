package de.polocloud.moduleapi

/**
 * Parsed form of a module's `module.yml` (read from the root of its jar). Available to
 * the module itself via [PolocloudModule.descriptor].
 *
 * ```yaml
 * name: cloudflare
 * version: 1.0.0
 * main: de.polocloud.modules.cloudflare.CloudflareModule
 * description: Registers this cluster's proxies with Cloudflare
 * authors: [ your-name ]
 * scope: SINGLE_ACTIVE
 * api-version: 3.0.0
 * depends: []
 * soft-depends: []
 * dependencies:
 *   - com.squareup.okhttp3:okhttp:4.12.0
 * ```
 */
data class ModuleDescriptor(
    val name: String,
    val version: String,
    val main: String,
    val description: String = "",
    val authors: List<String> = emptyList(),
    val scope: ModuleScope = ModuleScope.EVERY_NODE,
    /** Hard requirements — this module fails to load if any of these aren't loaded first. */
    val depends: List<String> = emptyList(),
    /** Load-order-only hints — missing ones are ignored instead of failing the load. */
    val softDepends: List<String> = emptyList(),
    /**
     * Minimum polocloud version (`major.minor.patch`) this module was built against — the
     * major version must match the running node's exactly (breaking changes bump major)
     * and the node must be at least this minor/patch. `null` skips the check entirely.
     */
    val apiVersion: String? = null,
    /**
     * Extra `group:artifact:version` Maven coordinates resolved against Maven Central and
     * added to this module's own classloader before it loads — an alternative to bundling
     * every library into the jar yourself. No transitive resolution: only exactly what's
     * listed here is downloaded.
     */
    val dependencies: List<String> = emptyList(),
)
