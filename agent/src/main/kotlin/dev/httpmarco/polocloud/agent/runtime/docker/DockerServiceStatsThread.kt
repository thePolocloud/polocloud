package dev.httpmarco.polocloud.agent.runtime.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Statistics
import dev.httpmarco.polocloud.agent.runtime.abstracts.AbstractServiceStatsThread
import dev.httpmarco.polocloud.common.math.convertBytesToMegabytes
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Thread implementation that fetches runtime statistics (CPU and memory usage)
 * for Docker containers and updates the corresponding service.
 *
 * This class relies on Docker's statistics API to collect resource usage data
 * and updates the values in the associated [DockerService].
 */
class DockerServiceStatsThread(
    private val client: DockerClient
) : AbstractServiceStatsThread<DockerService>() {

    /**
     * Detects the current resource usage of the given [DockerService]
     * and updates CPU and memory usage values.
     *
     * @param service The DockerService to update.
     */
    override fun detectService(service: DockerService) {
        val containerId = service.containerId ?: return

        val stats = getStats(containerId) ?: return

        // --- CPU ---
        val cpuPercent = calculateCpuUsage(stats) ?: return
        service.updateCpuUsage(cpuPercent)

        // --- Memory ---
        val usedMemory = stats.memoryStats?.usage ?: return
        service.updateMemoryUsage(convertBytesToMegabytes(usedMemory))
    }

    /**
     * Calculates the CPU usage percentage from Docker statistics.
     *
     * @param stats Docker statistics object.
     * @return CPU usage as a Double, or null if data is incomplete.
     */
    private fun calculateCpuUsage(stats: Statistics): Double? {
        val cpuUsage = stats.cpuStats?.cpuUsage?.totalUsage ?: return null
        val preCpuUsage = stats.preCpuStats?.cpuUsage?.totalUsage ?: return null
        val systemCpuUsage = stats.cpuStats?.systemCpuUsage ?: return null
        val preSystemCpuUsage = stats.preCpuStats?.systemCpuUsage ?: return null
        val onlineCpus = stats.cpuStats?.onlineCpus ?: return null

        val cpuDelta = cpuUsage - preCpuUsage
        val systemDelta = systemCpuUsage - preSystemCpuUsage

        val cpuPercent = if (systemDelta > 0 && cpuDelta > 0) {
            (cpuDelta.toDouble() / systemDelta.toDouble()) * onlineCpus.toDouble() * 100.0
        } else {
            0.0
        }

        return BigDecimal(cpuPercent)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Retrieves Docker statistics for a given container.
     *
     * @param containerId The container ID to request stats for.
     * @return Statistics object, or null if none were received.
     */
    private fun getStats(containerId: String): Statistics? {
        var result: Statistics? = null

        val callback = object : ResultCallback.Adapter<Statistics>() {
            override fun onNext(stats: Statistics) {
                result = stats
                this.close() // Close stream immediately after receiving one result
            }
        }

        client.statsCmd(containerId)
            .withNoStream(true)
            .exec(callback)
            .awaitCompletion() // Block until stream closes

        return result
    }
}
