package de.polocloud.node.group

import de.polocloud.database.EntryIdentifier
import de.polocloud.database.RepositoryName

@RepositoryName("groups")
data class Group (
    @EntryIdentifier val name: String,
    val memory: Int,
    val startThreshold: Double,
    val minOnline: Long,
    val maxOnline: Long,
    val platform: String,
    val version: String,
    // No default values on this constructor, even ergonomic ones (e.g. `static: Boolean =
    // false`) — SqlExecutor.resolveMeta() in polocloud-database picks
    // `declaredConstructors.first()` to reflectively rebuild rows, and a Kotlin default
    // parameter value makes the compiler emit a second, synthetic constructor
    // (real-arg-count + 2 for the bitmask/marker). `declaredConstructors` order is
    // unspecified, so `.first()` can pick either one — when it picks the synthetic one,
    // SqlExecutor.mapRow()'s N-arg call throws and every find()/findAll() on this table
    // silently returns empty (the exception is swallowed and logged). Callers that want
    // defaulting behavior should use named-arg construction at the call site instead.
    var static: Boolean,
    /**
     * Free-form key/value properties (e.g. `fallback=true`), persisted as JSON.
     *
     * Stored as a JSON string because the SQL layer maps each field to one column
     * and cannot persist a `Map` directly. Read [properties] for the decoded view.
     */
    val propertiesJson: String,
    /**
     * Ordered names of the templates applied to a service of this group on start,
     * persisted as JSON for the same reason as [propertiesJson]. Templates are copied
     * in this order into the service work directory, so a later entry's files win over
     * an earlier one's on conflict — see [de.polocloud.node.group.template.GroupTemplateService].
     */
    val templatesJson: String,
    /**
     * Names of the nodes ([de.polocloud.node.cluster.node.NodeData.name]) this group is
     * allowed to start services on, persisted as JSON for the same reason as
     * [templatesJson]. Empty means unrestricted — any online node is eligible. See
     * [de.polocloud.node.services.queue.GroupNodeEligibility].
     */
    val nodesJson: String,
) {

    /**
     * The decoded property map. Computed (no backing field) so it is not turned into
     * its own SQL column; the persisted representation is [propertiesJson].
     */
    val properties: MutableMap<String, String>
        get() = PropertyCodec.decode(propertiesJson)

    /**
     * The decoded, ordered template name list. Computed (no backing field) so it is not
     * turned into its own SQL column; the persisted representation is [templatesJson].
     */
    val templates: List<String>
        get() = TemplateCodec.decode(templatesJson)

    /**
     * The decoded node whitelist. Computed (no backing field) so it is not turned into
     * its own SQL column; the persisted representation is [nodesJson]. Empty means the
     * group may start on any online node.
     */
    val nodes: List<String>
        get() = TemplateCodec.decode(nodesJson)
}

/**
 * Convenience factory for a newly-created, non-static group with no properties, templates
 * or node restrictions yet. A top-level pseudo-constructor (not a companion-object member,
 * not a constructor default) — [Group] is persisted via reflection, and
 * `SqlExecutor.resolveMeta()` in `polocloud-database` builds the SQL column list off
 * `declaredFields`, which picks up a companion object's compiler-generated `Companion`
 * static field as a bogus extra column. Its value (`Group$Companion`) isn't `Serializable`,
 * so every insert fails with `NotSerializableException` even though the group still ends up
 * created in memory. `NodeData`/`Service` avoid a companion object for the same reason; this
 * keeps `Group` consistent with them while still being callable as `Group(name, memory, ...)`.
 * A constructor default would reintroduce a different reflection hazard instead — see
 * [Group.static]'s doc.
 */
fun Group(
    name: String,
    memory: Int,
    startThreshold: Double,
    minOnline: Long,
    maxOnline: Long,
    platform: String,
    version: String,
) = Group(
    name = name,
    memory = memory,
    startThreshold = startThreshold,
    minOnline = minOnline,
    maxOnline = maxOnline,
    platform = platform,
    version = version,
    static = false,
    propertiesJson = "{}",
    templatesJson = "[]",
    nodesJson = "[]",
)