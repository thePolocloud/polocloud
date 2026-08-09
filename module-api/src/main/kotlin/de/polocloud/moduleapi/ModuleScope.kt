package de.polocloud.moduleapi

/**
 * Declares how a module behaves when its jar is deployed to more than one node in the
 * same cluster — the deployment topology is a filesystem/config concern
 * (`local/modules/`), not something the module itself controls, so a module can't assume
 * it is the only instance running.
 */
enum class ModuleScope {

    /**
     * Default. Every node that has the jar loaded runs its own independent instance —
     * fine for anything stateless per-node, e.g. a REST module exposing that node's own
     * API on a local port.
     */
    EVERY_NODE,

    /**
     * Only one instance is ever active across the whole cluster at a time, even if the
     * jar sits in `local/modules/` on every node: [onEnable][PolocloudModule.onEnable] is
     * only called on the current cluster head ([ModuleNode.isHead]), every other instance
     * stays loaded but disabled (standby). If the head changes, the old head is disabled
     * and the new head is enabled automatically — no manual pinning to a specific node
     * needed. Use this for anything that would conflict with itself if run twice, e.g. a
     * module that registers this cluster's proxies with an external DNS/LB provider.
     */
    SINGLE_ACTIVE,
}
