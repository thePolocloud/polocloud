package de.polocloud.node.cluster.election

import de.polocloud.node.cluster.election.rpc.ElectionRpcClient
import de.polocloud.node.cluster.election.rpc.LeaderHeartbeatResult
import de.polocloud.node.cluster.election.rpc.RequestVoteResult
import de.polocloud.node.cluster.node.NodeData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * In-memory Raft-style leader-election state machine for one node.
 *
 * There is no replicated log here — actual cluster/service state already lives in the
 * shared [de.polocloud.node.cluster.node.NodeRepository]-backed database, which stays
 * the single source of truth for everything except "who is head right now". This FSM
 * only implements Raft's election subprotocol: randomized election timeouts, term
 * numbers, majority votes and a leader lease heartbeat — enough to guarantee at most
 * one head per term and to make a demoted/partitioned head step down as soon as it
 * hears about a newer term, instead of continuing to act on stale authority.
 *
 * All mutable state is only ever touched from [dispatcher] (single-threaded by
 * default), so no additional locking is needed despite RPC handlers and the election/
 * heartbeat loops all calling in concurrently.
 */
class ElectionState(
    private val localId: UUID,
    initialTerm: Long,
    initialVotedFor: UUID?,
    /**
     * All registered nodes including self — the quorum denominator and vote targets.
     * Not filtered by online state: a node that's merely unreachable still counts
     * towards the total, so a minority partition can't reach majority on its own.
     */
    private val members: () -> List<NodeData>,
    private val firstConnection: () -> kotlin.time.Instant,
    private val rpcClient: ElectionRpcClient,
    private val persistVote: (term: Long, votedFor: UUID?) -> Unit,
    /**
     * Side effect invoked only when the known leader actually changes (including
     * becoming leader ourselves), never on every repeated heartbeat — wires back into
     * NodeRepository's `head`/`electedAt` columns so existing DB readers keep working
     * unchanged, without turning every ~1s leader heartbeat into a DB write.
     */
    private val onLeaderChanged: (leaderId: UUID, term: Long) -> Unit,
    private val baseTimeout: Duration = 5.seconds,
    private val jitterRangeMillis: Long = 4_000L,
    private val heartbeatInterval: Duration = 1.seconds,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1),
) {

    enum class Role { FOLLOWER, CANDIDATE, LEADER }

    private val logger = LoggerFactory.getLogger(ElectionState::class.java)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    @Volatile
    var role: Role = Role.FOLLOWER
        private set

    @Volatile
    var currentTerm: Long = initialTerm
        private set

    @Volatile
    var currentLeader: UUID? = null
        private set

    private var votedFor: UUID? = initialVotedFor
    private var timerJob: Job? = null
    private var heartbeatJob: Job? = null

    fun start() {
        scope.launch { restartElectionTimer() }
    }

    fun stop() {
        timerJob?.cancel()
        heartbeatJob?.cancel()
        scope.cancel()
        rpcClient.close()
    }

    /**
     * Fast path for [NodeElectionService.onNodeCrashed]/[NodeElectionService.onHeadNodeLeft]:
     * this node already knows (via heartbeat monitoring) that the head is gone, so
     * there's no reason to wait out the rest of a randomized election timeout. Only
     * starts an election if this node is currently a plain follower — one already
     * mid-election or already leader ignores this, since Raft's own term/vote exchange
     * (not who triggers it) is what determines the real outcome.
     */
    fun triggerElection() {
        scope.launch {
            if (role == Role.FOLLOWER) startElection()
        }
    }

    suspend fun handleRequestVote(term: Long, candidateId: UUID): RequestVoteResult = withContext(dispatcher) {
        if (term > currentTerm) stepDown(term)

        val granted = term == currentTerm && (votedFor == null || votedFor == candidateId)
        if (granted) {
            votedFor = candidateId
            persistVote(currentTerm, votedFor)
            restartElectionTimer()
        }
        RequestVoteResult(currentTerm, granted)
    }

    suspend fun handleLeaderHeartbeat(term: Long, leaderId: UUID): LeaderHeartbeatResult = withContext(dispatcher) {
        if (term < currentTerm) {
            return@withContext LeaderHeartbeatResult(currentTerm, false)
        }
        if (term > currentTerm || role != Role.FOLLOWER) stepDown(term)
        val leaderChanged = currentLeader != leaderId
        currentLeader = leaderId
        if (leaderChanged) onLeaderChanged(leaderId, currentTerm)
        restartElectionTimer()
        LeaderHeartbeatResult(currentTerm, true)
    }

    private fun stepDown(term: Long) {
        heartbeatJob?.cancel()
        role = Role.FOLLOWER
        currentTerm = term
        votedFor = null
        currentLeader = null
        persistVote(currentTerm, votedFor)
    }

    private fun restartElectionTimer() {
        timerJob?.cancel()
        val timeout = nextTimeout()
        timerJob = scope.launch {
            delay(timeout)
            if (role != Role.LEADER) startElection()
        }
    }

    /**
     * Randomized election timeout, biased by cluster seniority: a node that joined
     * earlier gets a shorter base wait, so — all else equal — the most senior online
     * node tends to become a candidate first (the old strategy's tie-break, now applied
     * as timing instead of a direct pick). This is only a timing hint to reduce split
     * votes; correctness still comes entirely from the term/majority-vote exchange, so
     * a wrong guess here just costs one extra election round, never a safety violation.
     */
    private fun nextTimeout(): Duration {
        val seniorityBiasMillis = (System.currentTimeMillis() - firstConnection().toEpochMilliseconds())
            .coerceIn(0, 2_000)
        val jitter = (0..jitterRangeMillis).random()
        return baseTimeout - seniorityBiasMillis.milliseconds + jitter.milliseconds
    }

    /** `internal` (not `private`) so tests can drive one election round directly and deterministically, instead of only through the randomized timer/[triggerElection]. */
    internal suspend fun startElection() {
        if (role == Role.LEADER) return

        role = Role.CANDIDATE
        currentTerm += 1
        val term = currentTerm
        votedFor = localId
        persistVote(currentTerm, votedFor)
        logger.info("Starting election for term {}", term)

        val everyone = members()
        val majority = everyone.size / 2 + 1
        val peers = everyone.filter { it.id != localId }

        if (peers.isEmpty()) {
            becomeLeader(term)
            return
        }

        var votes = 1 // vote for self
        val responses = peers
            .map { peer -> scope.async { runCatching { rpcClient.requestVote(peer, term, localId) }.getOrNull() } }
            .awaitAll()

        if (role != Role.CANDIDATE || currentTerm != term) return // superseded while votes were in flight

        for (response in responses) {
            if (response == null) continue
            if (response.term > currentTerm) {
                stepDown(response.term)
                return
            }
            if (response.voteGranted) votes++
        }

        if (votes >= majority) {
            becomeLeader(term)
        } else {
            logger.info("Election for term {} did not reach a majority ({}/{}) — retrying after timeout", term, votes, majority)
            restartElectionTimer()
        }
    }

    private fun becomeLeader(term: Long) {
        role = Role.LEADER
        currentLeader = localId
        timerJob?.cancel()
        onLeaderChanged(localId, term)
        logger.info("Elected as head for term {}", term)

        heartbeatJob = scope.launch {
            while (isActive) {
                sendHeartbeats(term)
                delay(heartbeatInterval)
            }
        }
    }

    private suspend fun sendHeartbeats(term: Long) {
        if (role != Role.LEADER || currentTerm != term) return
        val peers = members().filter { it.id != localId }
        val responses = peers
            .map { peer -> scope.async { runCatching { rpcClient.leaderHeartbeat(peer, term, localId) }.getOrNull() } }
            .awaitAll()
        for (response in responses) {
            if (response != null && response.term > currentTerm) {
                stepDown(response.term)
                return
            }
        }
    }
}
