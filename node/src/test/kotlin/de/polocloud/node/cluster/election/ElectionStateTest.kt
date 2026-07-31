package de.polocloud.node.cluster.election

import de.polocloud.node.cluster.election.rpc.ElectionRpcClient
import de.polocloud.node.cluster.election.rpc.LeaderHeartbeatResult
import de.polocloud.node.cluster.election.rpc.RequestVoteResult
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.proto.NodeState
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Clock.System.now

class ElectionStateTest {

    private val selfId = UUID.randomUUID()
    private val peerAId = UUID.randomUUID()
    private val peerBId = UUID.randomUUID()

    private val persisted = mutableListOf<Pair<Long, UUID?>>()
    private val leaderChanges = mutableListOf<Pair<UUID, Long>>()
    private val created = mutableListOf<ElectionState>()

    @AfterEach
    fun cleanup() = created.forEach { it.stop() }

    private fun node(id: UUID, index: Int) = NodeData(
        id = id,
        index = index,
        hostname = "10.0.0.$index",
        port = 4240 + index,
        state = NodeState.ONLINE,
        version = "3",
        gitCommitHash = "abc",
        firstConnection = now(),
    )

    private fun newState(
        members: List<NodeData>,
        rpcClient: ElectionRpcClient = FakeRpcClient(),
        initialTerm: Long = 0,
        initialVotedFor: UUID? = null,
    ): ElectionState {
        val state = ElectionState(
            localId = selfId,
            initialTerm = initialTerm,
            initialVotedFor = initialVotedFor,
            members = { members },
            firstConnection = { now() },
            rpcClient = rpcClient,
            persistVote = { term, votedFor -> persisted.add(term to votedFor) },
            onLeaderChanged = { leaderId, term -> leaderChanges.add(leaderId to term) },
        )
        created += state
        return state
    }

    private class FakeRpcClient(
        private val voteResponses: Map<UUID, RequestVoteResult?> = emptyMap(),
        private val heartbeatResponses: Map<UUID, LeaderHeartbeatResult?> = emptyMap(),
    ) : ElectionRpcClient {
        override suspend fun requestVote(peer: NodeData, term: Long, candidateId: UUID) = voteResponses[peer.id]
        override suspend fun leaderHeartbeat(peer: NodeData, term: Long, leaderId: UUID) = heartbeatResponses[peer.id]
    }

    @Test
    fun `grants vote for a higher term and remembers it`() = runBlocking {
        val state = newState(members = listOf(node(selfId, 1)))
        val result = state.handleRequestVote(1, peerAId)

        assertTrue(result.voteGranted)
        assertEquals(1L, result.term)
        assertEquals(1L to peerAId, persisted.last())
    }

    @Test
    fun `rejects a request from a stale term`() = runBlocking {
        val state = newState(members = listOf(node(selfId, 1)), initialTerm = 5)
        val result = state.handleRequestVote(3, peerAId)

        assertFalse(result.voteGranted)
        assertEquals(5L, result.term)
    }

    @Test
    fun `does not grant a second vote to a different candidate in the same term`() = runBlocking {
        val state = newState(members = listOf(node(selfId, 1)))
        state.handleRequestVote(1, peerAId)
        val second = state.handleRequestVote(1, peerBId)

        assertFalse(second.voteGranted)
    }

    @Test
    fun `re-granting the same candidate in the same term is idempotent`() = runBlocking {
        val state = newState(members = listOf(node(selfId, 1)))
        state.handleRequestVote(1, peerAId)
        val again = state.handleRequestVote(1, peerAId)

        assertTrue(again.voteGranted)
    }

    @Test
    fun `leader heartbeat from a stale term is rejected`() = runBlocking {
        val state = newState(members = listOf(node(selfId, 1)), initialTerm = 5)
        val result = state.handleLeaderHeartbeat(3, peerAId)

        assertFalse(result.success)
        assertEquals(5L, result.term)
    }

    @Test
    fun `leader heartbeat adopts a newer term and records the leader exactly once`() = runBlocking {
        val state = newState(members = listOf(node(selfId, 1)))
        state.handleLeaderHeartbeat(1, peerAId)
        state.handleLeaderHeartbeat(1, peerAId) // repeat heartbeat — must not re-fire the side effect

        assertEquals(ElectionState.Role.FOLLOWER, state.role)
        assertEquals(peerAId, state.currentLeader)
        assertEquals(listOf(peerAId to 1L), leaderChanges)
    }

    @Test
    fun `becomes leader on its own when it is the only known member`() = runBlocking {
        val state = newState(members = listOf(node(selfId, 1)))
        state.startElection()

        assertEquals(ElectionState.Role.LEADER, state.role)
        assertEquals(selfId, state.currentLeader)
        assertEquals(listOf(selfId to 1L), leaderChanges)
    }

    @Test
    fun `becomes leader after winning a majority of votes`() = runBlocking {
        val members = listOf(node(selfId, 1), node(peerAId, 2), node(peerBId, 3))
        val rpc = FakeRpcClient(
            voteResponses = mapOf(
                peerAId to RequestVoteResult(1, true),
                peerBId to RequestVoteResult(1, false),
            )
        )
        val state = newState(members = members, rpcClient = rpc)
        state.startElection()

        assertEquals(ElectionState.Role.LEADER, state.role)
    }

    @Test
    fun `stays a candidate when it does not win a majority`() = runBlocking {
        val members = listOf(node(selfId, 1), node(peerAId, 2), node(peerBId, 3))
        val rpc = FakeRpcClient(
            voteResponses = mapOf(
                peerAId to RequestVoteResult(1, false),
                peerBId to RequestVoteResult(1, false),
            )
        )
        val state = newState(members = members, rpcClient = rpc)
        state.startElection()

        assertEquals(ElectionState.Role.CANDIDATE, state.role)
    }

    @Test
    fun `an unresponsive peer neither grants a vote nor blocks the election`() = runBlocking {
        val members = listOf(node(selfId, 1), node(peerAId, 2), node(peerBId, 3))
        val rpc = FakeRpcClient(voteResponses = mapOf(peerAId to RequestVoteResult(1, true))) // peerB never answers -> null
        val state = newState(members = members, rpcClient = rpc)
        state.startElection()

        assertEquals(ElectionState.Role.LEADER, state.role) // self + peerA already a majority of 3
    }

    @Test
    fun `steps down when a peer reports a higher term during voting`() = runBlocking {
        val members = listOf(node(selfId, 1), node(peerAId, 2))
        val rpc = FakeRpcClient(voteResponses = mapOf(peerAId to RequestVoteResult(9, false)))
        val state = newState(members = members, rpcClient = rpc)
        state.startElection()

        assertEquals(ElectionState.Role.FOLLOWER, state.role)
        assertEquals(9L, state.currentTerm)
    }

    @Test
    fun `a minority of total registered nodes cannot win even if every reachable peer votes yes`() = runBlocking {
        // 5 total members, only 1 peer reachable (2 votes out of 5 -> no majority of 3).
        val members = listOf(
            node(selfId, 1), node(peerAId, 2), node(peerBId, 3),
            node(UUID.randomUUID(), 4), node(UUID.randomUUID(), 5),
        )
        val rpc = FakeRpcClient(voteResponses = mapOf(peerAId to RequestVoteResult(1, true)))
        val state = newState(members = members, rpcClient = rpc)
        state.startElection()

        assertEquals(ElectionState.Role.CANDIDATE, state.role)
    }
}
