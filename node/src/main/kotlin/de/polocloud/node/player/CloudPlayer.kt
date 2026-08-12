package de.polocloud.node.player

import de.polocloud.database.EntryIdentifier
import de.polocloud.database.RepositoryName
import de.polocloud.node.group.PropertyCodec
import java.util.UUID

@RepositoryName("players")
// No default values on this constructor, even ergonomic ones — SqlExecutor.resolveMeta()
// in polocloud-database picks `declaredConstructors.first()` to reflectively rebuild
// rows, and a Kotlin default parameter value makes the compiler emit a second, synthetic
// constructor (real-arg-count + 2 for the bitmask/marker). `declaredConstructors` order
// is unspecified, so `.first()` can pick either one — when it picks the synthetic one,
// SqlExecutor.mapRow()'s N-arg call throws and every find()/findAll() on this table
// silently returns empty (the exception is swallowed and logged). See Service.kt/NodeData.kt
// for the same warning.
class CloudPlayer(
    @EntryIdentifier val id: UUID,
    var name: String,
    var skinValue: String,
    var skinSignature: String,
    /**
     * The full, raw set of Mojang profile properties this player logged in with,
     * persisted as JSON — the SQL layer maps each field to one column and cannot
     * persist a `Map` directly. Read [properties] for the decoded view.
     */
    var propertiesJson: String,
    var currentProxy: String,
    var currentServer: String?,
) {

    /**
     * The decoded property map. Computed (no backing field) so it is not turned into
     * its own SQL column; the persisted representation is [propertiesJson].
     */
    val properties: MutableMap<String, String>
        get() = PropertyCodec.decode(propertiesJson)
}
