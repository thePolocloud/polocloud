package de.polocloud.addons.hub

import de.polocloud.api.Polocloud
import de.polocloud.shared.service.Service
import de.polocloud.shared.service.ServiceState

/**
 * Platform-agnostic core of the hub addon (see [de.polocloud.bridge.BridgeInstance] for
 * the same shape on the bridge side). Owns the fallback-selection logic; a platform
 * module (Velocity today) only has to translate [connect]/[currentServerName]/[sendMessage]
 * into its own proxy API.
 */
abstract class HubAddon<P> {

    /** The name of the server [player] is currently connected to, or `null` if unknown. */
    abstract fun currentServerName(player: P): String?

    /** Connects [player] to [service]. Returns `false` if the platform has no server registered for it. */
    abstract fun connect(player: P, service: Service): Boolean

    /** Notifies [player], e.g. when no fallback service is currently available. */
    abstract fun sendMessage(player: P, message: String)

    /**
     * Sends [player] to the best available fallback service, excluding the server they're
     * already on so they are never routed right back to it. Notifies the player instead if
     * none is available.
     */
    fun sendToHub(player: P) {
        val target = bestFallback(excludeServiceName = currentServerName(player))
        if (target == null || !connect(player, target)) {
            sendMessage(player, "No hub server is currently available.")
        }
    }

    /**
     * Among running services flagged as fallback (own [Service.isFallback] flag or their
     * group's), the one in the highest priority tier with the fewest online players — same
     * selection rules as [de.polocloud.bridge.FallbackSelector.select]. `null` if none is eligible.
     */
    private fun bestFallback(excludeServiceName: String?): Service? {
        val fallbackGroups = Polocloud.groupService.findAll()
            .filter { it.isFallback() }
            .associate { it.name.lowercase() to it.fallbackPriority() }

        val candidates = Polocloud.serviceService.findAll().filter { service ->
            service.state == ServiceState.RUNNING &&
                (excludeServiceName == null || !service.name().equals(excludeServiceName, ignoreCase = true)) &&
                (service.group.lowercase() in fallbackGroups || service.isFallback())
        }

        val topPriority = candidates.maxOfOrNull { priorityOf(it, fallbackGroups) } ?: return null
        return candidates
            .filter { priorityOf(it, fallbackGroups) == topPriority }
            .minByOrNull { it.onlinePlayers }
    }

    /** A service's own fallback priority, or its group's if the service itself carries none. */
    private fun priorityOf(service: Service, fallbackGroups: Map<String, Int>): Int =
        if (service.isFallback()) service.fallbackPriority() else fallbackGroups[service.group.lowercase()] ?: 0
}