package de.polocloud.node.cluster.heartbeat

import de.polocloud.common.os.ApplicationResources
import de.polocloud.common.os.ResourceProvider
import de.polocloud.common.os.SystemResources
import de.polocloud.i18n.api.TranslationService
import de.polocloud.i18n.api.trError
import de.polocloud.node.core.environment.NodeEnvironment
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Service for generating and managing heartbeats for a cluster node.
 *
 * This service collects system metrics like CPU and memory usage and stores this
 * information periodically in the database. Additionally, old heartbeats are cleaned up.
 *
 * @property factory The database connection used for saving heartbeats.
 */
class NodeHeartBeatService {

    private val logger = LoggerFactory.getLogger(NodeHeartBeatService::class.java)
    private val systemResources: ResourceProvider = SystemResources
    private val applicationResources: ResourceProvider = ApplicationResources

    private var schedulerJob: Job? = null

    /**
     * Starts the heartbeat scheduler.
     *
     * @param interval The interval between heartbeats (default: 1 second).
     */
    fun startScheduler(interval: Duration = 1.seconds) {
        // Off the startup critical path: cleanUp() scans this node's entire heartbeat
        // history, which only grows the longer the node stays up between restarts, and
        // deletes stale rows one at a time. Blocking startup on it means every restart
        // gets slower as the node accumulates more uptime — running it in the background
        // instead means "Node is up" no longer waits on however large that table has
        // gotten since the last restart.
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { cleanUp() }
                .onFailure { logger.error("Heartbeat cleanup failed", it) }
        }

        val nodeId = NodeEnvironment.runtime.nodeId.get()

        schedulerJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                runCatching {
                    NodeHeartBeatRepository.save(generate())
                }.onFailure { exception ->
                    logger.trError("cluster", "cluster.heartbeat.save_failed", exception, "nodeId" to nodeId)
                }
                delay(interval)
            }
        }
    }

    /**
     * Stops the heartbeat scheduler if it is running.
     */
    fun stopScheduler() {
        schedulerJob?.cancel()
        schedulerJob = null
    }

    /**
     * Cleans up old heartbeats in the database.
     *
     * Heartbeats younger than 24 hours are kept, as well as at least one heartbeat per 10 minutes
     * in older data.
     */
    fun cleanUp() {
        val beats = NodeHeartBeatRepository.find(NodeEnvironment.runtime.nodeId.get()).sortedBy { it.heartBeatAt }
        if (beats.isEmpty()) return

        val now = Clock.System.now()
        val cutoff = now - 24.hours
        val toKeep = mutableSetOf<NodeHeartBeat>()

        // Keep at least one heartbeat per 10 minutes for older data
        beats.filter { it.heartBeatAt < cutoff }
            .groupBy { it.heartBeatAt.toEpochMilliseconds() / (10 * 60 * 1000) }
            .forEach { (_, group) ->
                group.minByOrNull { it.heartBeatAt }?.let { toKeep.add(it) }
            }

        // Keep all recent heartbeats
        beats.filter { it.heartBeatAt >= cutoff }.forEach { toKeep.add(it) }

        val toDelete = beats.filter { it !in toKeep }

        logger.info(TranslationService.tr("cluster", "cluster.heartbeat.cleanup"))

        toDelete.forEach { beat ->
            NodeHeartBeatRepository.delete(beat)
        }

        logger.info(
            TranslationService.tr(
                "cluster",
                "cluster.heartbeat.cleanup.complete",
                "deleted" to toDelete.size,
                "kept" to toKeep.size
            )
        )
    }

    /**
     * Generates a new heartbeat based on current system metrics.
     *
     * @return A [NodeHeartBeat] object with CPU usage and memory usage data.
     */
    fun generate(): NodeHeartBeat {
        val systemUsed = systemResources.usedMemory()
        val systemMax = systemResources.maxMemory()

        val appUsed = applicationResources.usedMemory()
        val appMax = applicationResources.maxMemory()

        val systemCpuUsage = systemResources.cpuUsage()
        val systemMemoryUsage = if (systemMax == 0.0) 0.0 else (systemUsed / systemMax) * 100.0

        val applicationCpuUsage = applicationResources.cpuUsage()
        val applicationMemoryUsage = if (appMax == 0.0) 0.0 else (appUsed / appMax) * 100.0

        return NodeHeartBeat(
            id = UUID.randomUUID().toString(),
            nodeId = NodeEnvironment.runtime.nodeId.get(),
            heartBeatAt = Clock.System.now(),
            systemCpuUsage = systemCpuUsage,
            systemMemoryUsage = systemMemoryUsage,
            applicationCpuUsage = applicationCpuUsage,
            applicationMemoryUsage = applicationMemoryUsage,
        )
    }
}