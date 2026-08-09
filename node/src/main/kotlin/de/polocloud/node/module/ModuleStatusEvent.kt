package de.polocloud.node.module

import de.polocloud.moduleapi.ModuleScope
import de.polocloud.shared.event.Event
import kotlinx.serialization.Serializable

/**
 * Fired by [ModuleManager] whenever a module's local enabled/disabled state changes, so
 * every node's [ClusterModuleRegistry] can build a cluster-wide picture of who's running
 * what — e.g. which node is currently the active [ModuleScope.SINGLE_ACTIVE] instance.
 *
 * Defined here rather than in `shared` on purpose: this event only ever needs to be
 * understood by other `node` processes (see [de.polocloud.shared.event.Event]'s doc on
 * addon-defined events), never by `api`/`bridge` consumers.
 */
@Serializable
data class ModuleStatusEvent(
    val nodeId: String,
    val nodeName: String,
    val moduleName: String,
    val moduleVersion: String,
    val scope: ModuleScope,
    val enabled: Boolean,
) : Event
