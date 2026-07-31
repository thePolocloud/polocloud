package de.polocloud.node.cluster.heartbeat

import de.polocloud.node.cluster.election.NodeElectionService
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.proto.NodeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock.System.now
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class NodeHeartBeatMonitor(
    private val electionService: NodeElectionService,
    // Heartbeats are saved every 1s by default (see NodeHeartBeatService), so this used
    // to give a node only ~2-3 missed beats of slack before being declared CRASHED —
    // easily tripped by a GC pause or a brief network blip, and a false positive here
    // isn't harmless: it can trigger a real head re-election. 15s tolerates a much more
    // realistic amount of jitter while still detecting a genuinely dead node well within
    // human-noticeable time. Both this and [tickInterval] are configurable (see
    // ClusterTimingConfiguration) since the right values depend on network conditions
    // this code can't assume.
    private val timeout: Duration = 15.seconds,
    private val tickInterval: Duration = 3.seconds,
) {

    private var job: Job? = null

    fun start() {
        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                checkAll()
                delay(tickInterval)
            }
        }
    }

    /** `internal` (not `private`) so tests can drive a check deterministically instead of only through the periodic [start] loop. */
    internal fun checkAll() {
        val now = now()
        val threshold = now - timeout

        NodeRepository.find(NodeState.ONLINE).forEach { node ->
            val latest = NodeHeartBeatRepository
                .find(node.id)
                .maxByOrNull { it.heartBeatAt }

            // A heartbeat older than our last contact with this node doesn't prove it's
            // dead on its own — e.g. right after a restart, stale heartbeat rows from the
            // previous run can still be lying around before the new scheduler writes a
            // fresh one. Fall back to lastConnection as the liveness reference in that
            // case (and when there's no heartbeat at all yet). That grace period must
            // still be bounded by the same timeout, though: without this fallback, a node
            // whose heartbeat scheduler never starts — or dies — never gets a heartbeat
            // row at all, `latest` stays null forever, and the old code exempted it from
            // crash detection permanently, leaving it stuck ONLINE and able to block
            // election and service placement indefinitely.
            val reference = if (latest != null && latest.heartBeatAt >= node.lastConnection) {
                latest.heartBeatAt
            } else {
                node.lastConnection
            }

            if (reference < threshold) {
                electionService.onNodeCrashed(node)
            }
        }
        // No separate "no active head" fallback here anymore: with the Raft-style
        // election in NodeElectionService/ElectionState, the absence of a head is a
        // self-correcting transient state — every follower's own randomized election
        // timeout fires on its own if no leader heartbeat arrives, so nothing external
        // needs to poke it. onNodeCrashed above still shortcuts that wait when this
        // monitor is what first notices the head is gone.
    }

    fun stop() = job?.cancel()
}