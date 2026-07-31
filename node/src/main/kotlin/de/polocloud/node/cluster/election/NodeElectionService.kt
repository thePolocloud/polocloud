package de.polocloud.node.cluster.election

import de.polocloud.node.cluster.election.rpc.GrpcElectionRpcClient
import de.polocloud.node.cluster.election.rpc.LeaderHeartbeatResult
import de.polocloud.node.cluster.election.rpc.RequestVoteResult
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.proto.NodeState
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Clock.System.now
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Orchestrates the Raft-style [ElectionState] FSM for this node and adapts it to
 * [NodeRepository]: quorum membership, term/vote persistence and the `head`/`electedAt`
 * projection are all backed by the shared DB, while the actual term/vote/heartbeat
 * exchange happens over gRPC via [ElectionState] — see that class for why a real
 * consensus protocol replaced the old "whoever notices first writes head=true locally"
 * approach (no locking in NodeRepository meant concurrent local elections could race,
 * and a demoted head had no way to learn it was superseded).
 */
class NodeElectionService {

    private val logger = LoggerFactory.getLogger(javaClass)

    private var localId: UUID? = null
    private var election: ElectionState? = null

    fun start(
        localId: UUID,
        baseTimeout: Duration = 5.seconds,
        jitterRangeMillis: Long = 4_000L,
        heartbeatInterval: Duration = 1.seconds,
    ) {
        this.localId = localId
        val self = NodeRepository.find(localId)

        val state = ElectionState(
            localId = localId,
            initialTerm = self?.term ?: 0,
            initialVotedFor = self?.votedFor,
            members = { NodeRepository.findAll() },
            firstConnection = { NodeRepository.find(localId)?.firstConnection ?: now() },
            rpcClient = GrpcElectionRpcClient(),
            persistVote = { term, votedFor -> persistVote(localId, term, votedFor) },
            onLeaderChanged = { leaderId, term -> applyLeader(leaderId, term) },
            baseTimeout = baseTimeout,
            jitterRangeMillis = jitterRangeMillis,
            heartbeatInterval = heartbeatInterval,
        )
        election = state
        state.start()
    }

    fun stop() {
        election?.stop()
        election = null
    }

    /** Whether this node currently believes itself to be head — for fencing checks before head-only actions (CA signing, forwarding-secret handoff, ...). */
    fun isHead(): Boolean {
        val id = localId ?: return false
        return election?.let { it.role == ElectionState.Role.LEADER && it.currentLeader == id } == true
    }

    /**
     * Marks [failed] as [NodeState.CRASHED] once its liveness reference (heartbeat or
     * [NodeData.lastConnection]) goes stale — regardless of which non-terminal state it
     * was in, not just [NodeState.ONLINE]: a node stuck in `STARTING`/`SYNCING` (died
     * before finishing startup) or `STOPPING` (killed mid-shutdown) needs to reach
     * `CRASHED` too, otherwise it never becomes eligible for [de.polocloud.node.cluster.node.NodePruneService]
     * and permanently inflates the election quorum denominator. No-op if already terminal.
     */
    fun onNodeCrashed(failed: NodeData) {
        if (failed.state != NodeState.CRASHED && failed.state != NodeState.STOPPED) {
            failed.state = NodeState.CRASHED
            NodeRepository.save(failed)
            logger.warn("Node ${failed.name()} marked as CRASHED")
        }

        // Skip the rest of our own election timeout — we already know the head is gone.
        // The actual winner is still decided by the term/majority-vote exchange, this
        // only speeds up failover.
        if (failed.head) election?.triggerElection()
    }

    /**
     * Called during graceful shutdown, before [stop]. A leaving node can't elect its
     * own successor — that would bypass quorum voting. Once [stop] cancels this node's
     * leader-heartbeat loop, peers notice the silence and elect a new head themselves
     * within one election timeout window (a few seconds); nothing further to do here.
     */
    fun onHeadNodeLeft() {
        if (isHead()) {
            logger.info("This node is leaving the cluster as head — peers will elect a new head")
        }
    }

    suspend fun handleRequestVote(term: Long, candidateId: UUID): RequestVoteResult =
        election?.handleRequestVote(term, candidateId) ?: RequestVoteResult(term, false)

    suspend fun handleLeaderHeartbeat(term: Long, leaderId: UUID): LeaderHeartbeatResult =
        election?.handleLeaderHeartbeat(term, leaderId) ?: LeaderHeartbeatResult(term, false)

    private fun persistVote(id: UUID, term: Long, votedFor: UUID?) {
        val node = NodeRepository.find(id) ?: return
        node.term = term
        node.votedFor = votedFor
        NodeRepository.save(node)
    }

    private fun applyLeader(leaderId: UUID, term: Long) {
        val nodes = NodeRepository.findAll()
        nodes.forEach { node ->
            val shouldBeHead = node.id == leaderId
            if (node.head == shouldBeHead) return@forEach
            node.head = shouldBeHead
            if (shouldBeHead) node.electedAt = now()
            NodeRepository.save(node)
        }
        logger.info("Node {} elected as new head for term {}", nodes.firstOrNull { it.id == leaderId }?.name() ?: leaderId, term)
    }
}
