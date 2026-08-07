package de.polocloud.node.cluster.heartbeat

import de.polocloud.database.EntryIdentifier
import de.polocloud.database.EntryRef
import de.polocloud.database.RepositoryName
import de.polocloud.node.cluster.node.NodeData
import java.util.UUID
import kotlin.time.Instant

/**
 * Represents a heartbeat from a node, including usage metrics.
 *
 * Used to carry a `tps` (ticks per second) field, computed from a `(50L..60L).random()`
 * fabrication — polocloud's node process is a headless orchestrator, not a Minecraft
 * server, so it has no real per-tick game loop to measure, and reporting random noise
 * as if it were one is worse than not reporting anything. Removed rather than replaced
 * with a stand-in metric (e.g. heartbeat-scheduler lag): none of this node's existing
 * loops run at a rate an operator would recognize as "tps", so relabeling their lag as
 * such would just be dishonest in a different shape. Consumers (`ClusterCommand`,
 * `InfoCommand`) now show no tps line at all instead of a fake number.
 *
 * @param id unique identifier of the heartbeat entry
 * @param nodeId the node that sent this heartbeat
 * @param heartBeatAt timestamp of the heartbeat (epoch millis)
 * @param cpuUsage CPU usage in percentage (0.0..100.0)
 * @param memoryUsage Memory usage in percentage (0.0..100.0)
 */

@RepositoryName("nodes_heartbeats")
data class NodeHeartBeat(
    @EntryIdentifier val id: String,
    @EntryRef(clazz = NodeData::class) val nodeId: UUID,
    val heartBeatAt: Instant,

    val systemCpuUsage: Double,
    val systemMemoryUsage: Double,

    val applicationCpuUsage: Double,
    val applicationMemoryUsage: Double,
) {

    init {
        require(systemCpuUsage in 0.0..100.0 && applicationCpuUsage in 0.0..100.0) { "CPU usage must be between 0 and 100" }
        require(systemMemoryUsage in 0.0..100.0 && applicationMemoryUsage in 0.0..100.0) { "Memory usage must be between 0 and 100" }
    }

    fun diff(other: NodeHeartBeat) = NodeHeartBeat(
        id, nodeId,
        heartBeatAt,
        systemCpuUsage - other.systemCpuUsage,
        systemMemoryUsage - other.systemMemoryUsage,
        applicationCpuUsage - other.applicationCpuUsage,
        applicationMemoryUsage - other.applicationMemoryUsage,
    )
}