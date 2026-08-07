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
    /** How often a node saves its own CPU/memory heartbeat row. */
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
) {

    // Validated eagerly at construction — which, for the `NodeConfigurations` this is
    // nested under, means at config-file load time (see ConfigurationHolder.loadFromDisk)
    // — so a bad `config.json` fails node startup with a clear message instead of letting
    // a non-positive interval busy-loop a scheduler, or an inverted timeout/heartbeat
    // ordering make the cluster instantly (or never) flap between states.
    init {
        require(heartbeatIntervalMillis > 0) {
            "cluster.timing.heartbeatIntervalMillis must be positive, was $heartbeatIntervalMillis"
        }
        require(heartbeatMonitorTickMillis > 0) {
            "cluster.timing.heartbeatMonitorTickMillis must be positive, was $heartbeatMonitorTickMillis"
        }
        require(heartbeatCrashTimeoutMillis > 0) {
            "cluster.timing.heartbeatCrashTimeoutMillis must be positive, was $heartbeatCrashTimeoutMillis"
        }
        require(electionBaseTimeoutMillis > 0) {
            "cluster.timing.electionBaseTimeoutMillis must be positive, was $electionBaseTimeoutMillis"
        }
        require(electionJitterRangeMillis >= 0) {
            "cluster.timing.electionJitterRangeMillis must not be negative, was $electionJitterRangeMillis"
        }
        require(leaderHeartbeatIntervalMillis > 0) {
            "cluster.timing.leaderHeartbeatIntervalMillis must be positive, was $leaderHeartbeatIntervalMillis"
        }
        require(heartbeatCrashTimeoutMillis > heartbeatIntervalMillis) {
            "cluster.timing.heartbeatCrashTimeoutMillis ($heartbeatCrashTimeoutMillis) must be greater than " +
                "heartbeatIntervalMillis ($heartbeatIntervalMillis) — otherwise a node's own heartbeat cadence " +
                "can't keep it under the timeout meant to detect it going stale, and every node flaps to CRASHED."
        }
        require(electionBaseTimeoutMillis > leaderHeartbeatIntervalMillis) {
            "cluster.timing.electionBaseTimeoutMillis ($electionBaseTimeoutMillis) must be greater than " +
                "leaderHeartbeatIntervalMillis ($leaderHeartbeatIntervalMillis) — otherwise followers time out " +
                "and start a new election before they can even see the current leader's heartbeat."
        }
    }
}
