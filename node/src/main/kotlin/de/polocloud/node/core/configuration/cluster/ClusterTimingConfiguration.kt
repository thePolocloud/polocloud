package de.polocloud.node.core.configuration.cluster

import kotlinx.serialization.Serializable

/**
 * Tunables for the cluster's liveness/failover timing, all in milliseconds (plain
 * `Long`s rather than `kotlin.time.Duration` — simpler to hand-edit in `config.json`,
 * converted to `Duration` where each value is consumed).
 *
 * The defaults match what used to be hardcoded: 1s heartbeats, a 3s monitor tick, a 15s
 * crash timeout (see [de.polocloud.node.cluster.heartbeat.NodeHeartBeatMonitor] for why
 * 15s specifically), and a 5s+jitter Raft election timeout with a 1s leader lease
 * heartbeat (see [de.polocloud.node.cluster.election.ElectionState]).
 */
@Serializable
data class ClusterTimingConfiguration(
    /** How often a node saves its own CPU/memory/TPS heartbeat row. */
    var heartbeatIntervalMillis: Long = 1_000,
    /** How often [de.polocloud.node.cluster.heartbeat.NodeHeartBeatMonitor] re-checks every ONLINE node's liveness. */
    var heartbeatMonitorTickMillis: Long = 3_000,
    /** How long a node's heartbeat (or, absent one, its last contact) may go stale before it's declared CRASHED. */
    var heartbeatCrashTimeoutMillis: Long = 15_000,
    /** Base Raft election timeout before a follower with no leader heartbeat becomes a candidate. */
    var electionBaseTimeoutMillis: Long = 5_000,
    /** Upper bound of the random jitter added on top of [electionBaseTimeoutMillis], to reduce split votes between simultaneously-timing-out followers. */
    var electionJitterRangeMillis: Long = 4_000,
    /** How often an elected head sends its Raft leader-lease heartbeat to peers. */
    var leaderHeartbeatIntervalMillis: Long = 1_000,
)
