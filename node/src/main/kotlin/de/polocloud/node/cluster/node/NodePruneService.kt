package de.polocloud.node.cluster.node

import de.polocloud.node.cluster.heartbeat.NodeHeartBeatRepository
import de.polocloud.node.core.environment.NodeEnvironment
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
 * Only the current head node prunes on each tick, so peers don't race each other deleting
 * the same rows.
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
        // Live in-memory Raft role, not the NodeRepository.head DB projection — see
        // ServiceIdentityProvisioner.isHead for why that column alone isn't trustworthy
        // enough to gate a head-only action on.
        if (!NodeEnvironment.runtime.electionService.isHead()) return

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
