package de.polocloud.api.group

import de.polocloud.shared.property.Properties

/**
 * Fluent builder used to create or edit a [Group].
 *
 * Obtain an instance from [GroupService.create] (name pre-filled) or
 * [GroupService.edit] (every field pre-filled from the current group, so fields
 * left untouched by the editor are resubmitted unchanged rather than reset to a
 * default). Configure the fields, then call [submit] to send the change to the
 * cluster — [submit] returns the persisted group.
 */
class GroupBuilder internal constructor(
    initial: Group? = null,
    private val submitter: (Group) -> Group,
) {

    private var name: String = initial?.name ?: ""
    private var memory: Int = initial?.memory ?: 512
    private var startThreshold: Double = initial?.startThreshold ?: 0.0
    private var minOnline: Long = initial?.minOnline ?: 0
    private var maxOnline: Long = initial?.maxOnline ?: 1
    private var platform: String = initial?.platform ?: ""
    private var version: String = initial?.version ?: ""
    private val properties: Properties = Properties().apply {
        initial?.properties?.asMap()?.forEach { (key, value) -> set(key, value) }
    }
    private val templates: MutableList<String> = initial?.templates?.toMutableList() ?: mutableListOf()
    private val nodes: MutableList<String> = initial?.nodes?.toMutableList() ?: mutableListOf()

    fun name(name: String): GroupBuilder = apply { this.name = name }
    fun memory(memory: Int): GroupBuilder = apply { this.memory = memory }
    fun startThreshold(startThreshold: Double): GroupBuilder = apply { this.startThreshold = startThreshold }
    fun minOnline(minOnline: Long): GroupBuilder = apply { this.minOnline = minOnline }
    fun maxOnline(maxOnline: Long): GroupBuilder = apply { this.maxOnline = maxOnline }
    fun platform(platform: String): GroupBuilder = apply { this.platform = platform }
    fun version(version: String): GroupBuilder = apply { this.version = version }

    /** Sets a single key/value property on the group. */
    fun property(key: String, value: String): GroupBuilder = apply { this.properties.set(key, value) }

    /** Adds all entries of [properties] to the group's properties. */
    fun properties(properties: Map<String, String>): GroupBuilder =
        apply { properties.forEach { (key, value) -> this.properties.set(key, value) } }

    /** Marks this group as a fallback target for the bridge (`fallback=true`). */
    fun fallback(fallback: Boolean = true): GroupBuilder =
        apply { this.properties.set(Properties.FALLBACK, fallback.toString()) }

    /**
     * Marks this group as a fallback target and ranks it against other fallback
     * groups: when a proxy has to pick among several, higher [priority] values are
     * tried first. Implies [fallback].
     */
    fun fallbackPriority(priority: Int): GroupBuilder =
        apply {
            this.properties.set(Properties.FALLBACK, true.toString())
            this.properties.set(Properties.FALLBACK_PRIORITY, priority.toString())
        }

    /**
     * Appends a template (by name) to the ordered list applied to a service of this group
     * on start. Templates are copied in this order, so a later entry's files win over an
     * earlier one's on conflict.
     */
    fun template(name: String): GroupBuilder = apply { this.templates.add(name) }

    /**
     * Appends the given templates (by name) to the ordered list applied to a service of
     * this group on start.
     */
    fun templates(vararg names: String): GroupBuilder = apply { this.templates.addAll(names) }

    /** Restricts this group to a single node (by its cluster name, e.g. `node-1`). */
    fun node(name: String): GroupBuilder = apply { this.nodes.add(name) }

    /**
     * Restricts this group to the given nodes (by their cluster names). Leave unset (or
     * empty) to allow the group to start on any online node.
     */
    fun nodes(vararg names: String): GroupBuilder = apply { this.nodes.addAll(names) }

    internal fun toGroup(): Group {
        require(name.isNotBlank()) { "Group name must be set" }
        return Group(
            name, memory, startThreshold, minOnline, maxOnline, platform, version,
            properties, templates.toList(), nodes.toList(),
        )
    }

    /**
     * Sends the configured group to the cluster and returns the persisted group.
     */
    fun submit(): Group = submitter(toGroup())
}
