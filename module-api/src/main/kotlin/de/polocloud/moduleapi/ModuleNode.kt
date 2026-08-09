package de.polocloud.moduleapi

import de.polocloud.moduleapi.config.ModuleConfig
import java.io.File
import java.util.UUID

/**
 * The local node a module is running on, injected by the loader before
 * [PolocloudModule.onLoad] runs.
 *
 * Cluster-wide operations (groups, services, cross-node events) aren't exposed here —
 * modules run in-process with the node, so they reach those the same way any other SDK
 * consumer does: through [de.polocloud.api.Polocloud] directly (`module-api` depends on
 * `api`, so it's always on the classpath). [ModuleNode] only covers facts that are
 * specific to *this* node and not part of that cluster-facing SDK.
 */
interface ModuleNode {

    /** This node's cluster-unique id. */
    val nodeId: UUID

    /** This node's configured name. */
    val nodeName: String

    /**
     * A private, per-module directory (`local/modules/data/<module name>/`) the module
     * can freely read/write its own config and state in. Created on first access.
     */
    val dataFolder: File

    /**
     * Whether this node is currently the elected cluster head.
     *
     * For [ModuleScope.SINGLE_ACTIVE] modules the loader already gates
     * [PolocloudModule.onEnable]/[PolocloudModule.onDisable] on this, flipping them
     * automatically on failover — but a long-running module should still re-check before
     * any action that would conflict if two nodes did it at once (e.g. right before
     * calling an external API), since head status can change between enable and the
     * action itself.
     */
    fun isHead(): Boolean

    /**
     * A YAML-backed `config.yml` in [dataFolder], typed as [T]. See
     * [de.polocloud.moduleapi.config.config] for the more convenient reified form
     * (`node.config { MyConfig() }`).
     *
     * @param default supplies the initial value written to disk the first time this is called.
     */
    fun <T : Any> config(type: Class<T>, default: () -> T): ModuleConfig<T>
}
