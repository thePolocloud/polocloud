package de.polocloud.modules.status

import de.polocloud.shared.service.Service
import de.polocloud.shared.service.ServiceState

/**
 * Pure computation of a single group's published [GroupStatus] from its config entry
 * and the cluster's current services of that group — kept free of the Polocloud SDK/HTTP
 * so it's testable without a live node.
 */
object StatusEvaluator {

    fun evaluate(name: String, config: StatusGroupConfig, services: List<Service>): GroupStatus {
        val running = services.filter { it.state == ServiceState.RUNNING }
        val availability = when {
            !config.available -> GroupAvailability.MAINTENANCE
            running.isNotEmpty() -> GroupAvailability.ONLINE
            else -> GroupAvailability.OFFLINE
        }
        return GroupStatus(
            name = name,
            displayName = config.displayName.ifBlank { name },
            availability = availability,
            onlineServices = running.size,
            onlinePlayers = running.sumOf { it.onlinePlayers },
            maxPlayers = running.sumOf { it.maxPlayers },
        )
    }
}
