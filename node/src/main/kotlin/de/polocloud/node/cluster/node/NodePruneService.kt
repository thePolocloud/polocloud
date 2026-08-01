package de.polocloud.node.cluster.node

import de.polocloud.node.cluster.heartbeat.NodeHeartBeatRepository
import de.polocloud.proto.NodeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Periodically deletes [NodeData] rows stuck in a terminal state ([NodeState.CRASHED] /
 * [NodeState.STOPPED]) for longer than [staleAfter]. Nothing else prunes them — a crashed
 * or stopped node is only ever `save`d into that state (see
 * [de.polocloud.node.cluster.election.NodeElectionService.onNodeCrashed] /
 * [LocalNodeContainer.markStopped]), never removed — so without this the `nodes` table
 * would grow forever, unlike heartbeats which [de.polocloud.node.cluster.heartbeat.NodeHeartBeatService]
 * already cleans up.
 *
 * Every node prunes independently on each tick — deliberately not gated on [NodeElectionService.isHead]
 * despite that seeming like the natural fit (peers racing to delete the same rows sounds
 * wasteful): [de.polocloud.node.cluster.election.ElectionState]'s quorum denominator is
 * *every* registered node, stale or not (see its `members` doc comment — that's what stops a
 * minority partition electing itself). A stale row therefore isn't just clutter: while it
 * lingers, it can permanently deny the surviving node(s) a majority, so no node *can* ever
 * become head again — which would make a head-gated prune a deadlock (recovering quorum
 * requires pruning, pruning requires quorum) instead of just an inefficiency. A concurrent
 * `NodeRepository.delete` of an already-deleted row is a harmless no-op, so the actual
 * downside of dropping the gate is a few redundant deletes, never double the data loss.
 */
class NodePruneService(
    private val staleAfter: Duration = 1.hours,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private var job: Job? = null

    fun start(interval: Duration = 10.minutes) {
        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                runCatching { pruneStaleNodes() }
                    .onFailure { logger.error("Node pruning failed", it) }
                delay(interval)
            }
        }
    }

    fun stop() = job?.cancel()

    private fun pruneStaleNodes() {
        val cutoff = Clock.System.now() - staleAfter
        val stale = NodeRepository.findAll().filter {
            (it.state == NodeState.CRASHED || it.state == NodeState.STOPPED) && it.lastConnection < cutoff
        }
        if (stale.isEmpty()) return

        stale.forEach { node ->
            NodeHeartBeatRepository.find(node.id).forEach(NodeHeartBeatRepository::delete)
            NodeRepository.delete(node)
        }

        logger.info("Pruned {} stale node(s): {}", stale.size, stale.joinToString { it.name() })
    }
}
