package de.polocloud.node.services

import de.polocloud.common.math.convertBytesToMegabytes
import oshi.SystemInfo
import oshi.software.os.OSProcess
import java.util.concurrent.ConcurrentHashMap

/**
 * Samples CPU and memory usage of individual service OS processes via oshi.
 *
 * CPU load between two points in time needs a previous [OSProcess] snapshot of the same
 * process (see [OSProcess.getProcessCpuLoadBetweenTicks]) — mirrors
 * [de.polocloud.common.os.SystemResources]'s single `prevTicks` field, just keyed per PID
 * instead of a singleton. [OSProcess.getStartTime] guards against a reused PID being
 * mistaken for the process that previously held it.
 */
class ServiceResourceSampler {

    private val systemInfo = SystemInfo()
    private val previousSnapshots = ConcurrentHashMap<Long, OSProcess>()

    /** Null if [pid] is not a process oshi can currently see (already exited, or an unsupported OS). */
    fun sample(pid: Long): ServiceResourceUsage? {
        val process = systemInfo.operatingSystem.getProcess(pid.toInt())
        if (process == null) {
            previousSnapshots.remove(pid)
            return null
        }

        val previous = previousSnapshots[pid]?.takeIf { it.startTime == process.startTime }
        previousSnapshots[pid] = process

        val cpuLoad = if (previous != null) {
            process.getProcessCpuLoadBetweenTicks(previous)
        } else {
            // First sample for this PID: no baseline yet, fall back to the lifetime average.
            process.processCpuLoadCumulative
        }

        return ServiceResourceUsage(
            cpuUsage = (cpuLoad * 100.0).coerceIn(0.0, 100.0),
            usedMemory = convertBytesToMegabytes(process.residentMemory),
        )
    }

    /**
     * Drops every retained snapshot whose PID isn't in [alivePids] — called once per tick so a
     * stopped service's snapshot doesn't linger in memory for the rest of the node's uptime.
     */
    fun retainOnly(alivePids: Set<Long>) {
        previousSnapshots.keys.retainAll(alivePids)
    }
}

data class ServiceResourceUsage(val cpuUsage: Double, val usedMemory: Double)