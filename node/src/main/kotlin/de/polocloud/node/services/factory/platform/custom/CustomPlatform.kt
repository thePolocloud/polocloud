package de.polocloud.node.services.factory.platform.custom

import de.polocloud.database.EntryIdentifier
import de.polocloud.database.RepositoryName

/**
 * A hand-defined platform created by an operator via `platform setup`, persisted so it
 * survives a node restart — unlike built-in platforms, which are freely rebuilt on every
 * [de.polocloud.node.services.factory.PlatformService.load] from the downloaded template
 * bundle.
 *
 * @param name Unique platform identifier, disjoint from every built-in platform name
 *             (enforced at creation, see [CustomPlatformService.create]).
 * @param type Platform role: "SERVER" or "PROXY", matching [de.polocloud.node.services.factory.platform.Platform.type].
 * @param language Runtime language used to launch the platform (e.g. "JAVA").
 * @param versionsJson Attached versions, persisted as JSON — see [CustomPlatformVersionCodec]
 *                      for why this can't be a `List` column directly.
 */
@RepositoryName("custom_platforms")
// No default values on this constructor, even ergonomic ones — SqlExecutor.resolveMeta()
// in polocloud-database picks `declaredConstructors.first()` to reflectively rebuild
// rows, and a Kotlin default parameter value makes the compiler emit a second, synthetic
// constructor (real-arg-count + 2 for the bitmask/marker). `declaredConstructors` order
// is unspecified, so `.first()` can pick either one — when it picks the synthetic one,
// SqlExecutor.mapRow()'s N-arg call throws and every find()/findAll() on this table
// silently returns empty (the exception is swallowed and logged).
data class CustomPlatform(
    @EntryIdentifier val name: String,
    val type: String,
    val language: String,
    val versionsJson: String,
) {

    /** The decoded version list. Computed (no backing field) — the persisted form is [versionsJson]. */
    val versions: List<CustomPlatformVersion>
        get() = CustomPlatformVersionCodec.decode(versionsJson)
}
