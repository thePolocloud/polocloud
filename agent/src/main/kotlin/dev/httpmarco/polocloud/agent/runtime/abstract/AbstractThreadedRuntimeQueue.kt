package dev.httpmarco.polocloud.agent.runtime.abstract

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.groups.AbstractGroup
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.shutdownProcess
import dev.httpmarco.polocloud.shared.properties.PRIORITY
import dev.httpmarco.polocloud.v1.services.ServiceState

/**
 * This runtime queue runs continuously in a dedicated thread.
 * It checks all groups and starts new services when required.
 *
 * Groups are processed in descending priority order.
 */
open class AbstractThreadedRuntimeQueue : Thread("polocloud-local-runtime-queue") {

    override fun run() {
        try {
            val runtime = Agent.runtime

            while (!isInterrupted && !shutdownProcess()) {

                // Sort all groups by PRIORITY property (higher priority = processed first)
                runtime.groupStorage().findAll()
                    .sortedByDescending { it.properties.get(PRIORITY) ?: -1 }
                    .forEach { group ->

                        // Determine how many services need to be started for this group
                        val required = requiredServersThatStart(group)

                        repeat(required) {
                            // Respect maxOnlineService limit (-1 = unlimited)
                            if (group.services().size >= group.maxOnlineService && group.maxOnlineService != -1) {
                                return@repeat
                            }

                            // Create and boot a new service instance
                            val service = runtime.factory().generateInstance(group)
                            runtime.serviceStorage().deployAbstractService(service)
                            runtime.factory().bootApplication(service)
                        }
                    }

                // Small delay before the next evaluation tick
                sleep(1000)
            }

        } catch (_: InterruptedException) {
            // Thread was interrupted intentionally → exit gracefully
        } catch (e: Throwable) {
            // Log unexpected errors
            logger.throwable(e)
        }
    }

    /**
     * Calculates how many new services should be started for a group.
     *
     * Step 1: Ensure minOnlineService is satisfied.
     * Step 2: Optionally start additional services based on average load:
     *         If the player usage percentage exceeds the configured threshold,
     *         one additional service is scheduled to start.
     */
    private fun requiredServersThatStart(group: AbstractGroup): Int {

        // Step 1: Ensure minimum online service count
        var required = (group.minOnlineService - group.serviceCount()).coerceAtLeast(0)

        // Compute average max players (only valid values)
        val avgMaxPlayers = group.services()
            .asSequence()
            .filter { it.maxPlayerCount != -1 }
            .map { it.maxPlayerCount }
            .average()

        // Compute average online players (only valid values)
        val avgOnlinePlayers = group.services()
            .asSequence()
            .filter { it.playerCount != -1 }
            .map { it.playerCount }
            .average()

        // Validate averages
        val averagesAreValid = avgMaxPlayers > 0 && avgOnlinePlayers >= 0

        // Step 2: If minimum is satisfied, evaluate dynamic scaling
        if (required <= 0 && averagesAreValid && group.services().all { it.state == ServiceState.ONLINE }) {

            // Current utilization percentage
            val currentUsagePercent = (avgOnlinePlayers / avgMaxPlayers) * 100.0

            // Start an extra service if utilization exceeds the threshold
            if (currentUsagePercent >= group.percentageToStartNewService) {
                required += 1
            }
        }

        return required
    }
}
