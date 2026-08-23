package de.polocloud.modules.status

import de.polocloud.api.Polocloud
import de.polocloud.moduleapi.ModuleNode
import de.polocloud.moduleapi.config.ModuleConfig

/**
 * Builds the published status view on demand from the live config and the cluster's
 * current services — nothing is cached, so every request reflects the current state.
 */
class StatusService(
    private val node: ModuleNode,
    private val config: ModuleConfig<StatusConfig>,
) {

    fun snapshot(): StatusSnapshot {
        val groups = config.value.groups.map { (name, groupConfig) ->
            StatusEvaluator.evaluate(name, groupConfig, Polocloud.serviceService.findByGroup(name))
        }
        return StatusSnapshot(node = node.nodeName, generatedAt = System.currentTimeMillis(), groups = groups)
    }

    fun group(name: String): GroupStatus? {
        val groupConfig = config.value.groups[name] ?: return null
        return StatusEvaluator.evaluate(name, groupConfig, Polocloud.serviceService.findByGroup(name))
    }
}
