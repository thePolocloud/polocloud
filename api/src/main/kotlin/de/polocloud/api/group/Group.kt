package de.polocloud.api.group

import de.polocloud.shared.property.Properties
import de.polocloud.shared.property.PropertyHolder

data class Group (
    val name: String,
    val memory: Int,
    val startThreshold: Double,
    val minOnline: Long,
    val maxOnline: Long,
    val platform: String,
    val version: String,
    /** Free-form key/value properties attached to this group (e.g. `fallback=true`). */
    override val properties: Properties = Properties(),
    /**
     * Ordered names of the templates applied to a service of this group on start. Templates
     * are copied in this order into the service work directory, so a later entry's files win
     * over an earlier one's on conflict.
     */
    val templates: List<String> = emptyList(),
    /** Names of the nodes this group may start on. Empty means any online node is eligible. */
    val nodes: List<String> = emptyList(),
) : PropertyHolder()
