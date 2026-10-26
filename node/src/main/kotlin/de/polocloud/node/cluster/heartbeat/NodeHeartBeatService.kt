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
import kotlin.time.Duration.Companion.minutes
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

    // Tracked (unlike a bare launch{}) so stopScheduler() can wait for it — otherwise a
    // still-running cleanUp() keeps issuing one DELETE per stale row against a database
    // that NodeLifecycle.shutdown() has, by that point, already closed, producing a
    // cascade of "HikariDataSource has been closed" errors instead of a clean shutdown.
    private var cleanupJob: Job? = null

    /**
     * Starts the heartbeat scheduler.
     *
     * @param interval The interval between heartbeats (default: 1 second).
     */
    fun startScheduler(interval: Duration = 1.seconds, cleanupInterval: Duration = CLEANUP_INTERVAL) {
        cleanupJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                runCatching { cleanUp { isActive } }
                    .onFailure { logger.error("Heartbeat cleanup failed", it) }
                delay(cleanupInterval)
            }
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
     * Stops the heartbeat scheduler if it is running, and waits for a still-in-flight
     * [cleanUp] to finish (bounded by [CLEANUP_SHUTDOWN_TIMEOUT]) before returning — see
     * [cleanupJob]'s doc for why this must happen before the caller closes the database.
     */
    fun stopScheduler() {
        schedulerJob?.cancel()
        schedulerJob = null

        cleanupJob?.let { job ->
            job.cancel()
            runBlocking {
                if (withTimeoutOrNull(CLEANUP_SHUTDOWN_TIMEOUT) { job.join() } == null) {
                    logger.warn("Heartbeat cleanup did not finish within {} during shutdown — it may still be writing to a closing database.", CLEANUP_SHUTDOWN_TIMEOUT)
                }
            }
        }
        cleanupJob = null
    }

    /**
     * Cleans up old heartbeats in the database.
     *
     * Heartbeats younger than [RETENTION_FULL] are kept untouched. Older data is thinned
     * to the earliest heartbeat per [RETENTION_BUCKET] window. Idempotent, so it's safe to
     * run repeatedly: already-thinned buckets contain a single row and produce no deletes.
     *
     * @param shouldContinue polled between deletes; returning `false` aborts the pass early.
     */
    fun cleanUp(shouldContinue: () -> Boolean = { true }) {
        val cutoff = Clock.System.now() - RETENTION_FULL
        val bucketMillis = RETENTION_BUCKET.inWholeMilliseconds

        // Only rows past the cutoff are candidates; recent ones are never even grouped.
        val all = NodeHeartBeatRepository.find(NodeEnvironment.runtime.nodeId.get())
        val toDelete = all
            .filter { it.heartBeatAt < cutoff }
            .groupBy { it.heartBeatAt.toEpochMilliseconds() / bucketMillis }
            .values
            .flatMap { bucket -> bucket.sortedBy { it.heartBeatAt }.drop(1) }

        if (toDelete.isEmpty()) return

        logger.info(TranslationService.tr("cluster", "cluster.heartbeat.cleanup"))

        var deleted = 0
        for (beat in toDelete) {
            // Bail out promptly on shutdown instead of deleting against a closing database.
            if (!shouldContinue()) return
            // One failed row must not abort the rest of the pass; it's retried next run.
            runCatching { NodeHeartBeatRepository.delete(beat) }
                .onSuccess { deleted++ }
                .onFailure { logger.debug("Could not delete heartbeat {}", beat.id, it) }
        }

        logger.info(
            TranslationService.tr(
                "cluster",
                "cluster.heartbeat.cleanup.complete",
                "deleted" to deleted,
                "kept" to all.size - deleted
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

    private companion object {
        val CLEANUP_SHUTDOWN_TIMEOUT = 15.seconds
        val CLEANUP_INTERVAL = 10.minutes
        val RETENTION_FULL = 24.hours
        val RETENTION_BUCKET = 10.minutes
    }
}