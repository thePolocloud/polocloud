package de.polocloud.node.cluster.election.rpc

import de.polocloud.node.cluster.node.NodeData
import java.util.UUID

data class RequestVoteResult(val term: Long, val voteGranted: Boolean)

data class LeaderHeartbeatResult(val term: Long, val success: Boolean)

/**
 * Outbound transport for the Raft-style election RPCs. Kept as an interface (rather
 * than a concrete gRPC class referenced directly by [de.polocloud.node.cluster.election.ElectionState])
 * so the election state machine can be unit-tested without a real network — see
 * [de.polocloud.node.event.ClusterEventRelay] for the equivalent pattern used for event relay.
 *
 * A `null` result means the peer was unreachable or the call failed; callers must treat
 * that the same as "no answer" (a peer that doesn't respond doesn't grant a vote, but
 * also can't cause a spurious step-down).
 */
interface ElectionRpcClient {
    suspend fun requestVote(peer: NodeData, term: Long, candidateId: UUID): RequestVoteResult?
    suspend fun leaderHeartbeat(peer: NodeData, term: Long, leaderId: UUID): LeaderHeartbeatResult?

    /** Releases any cached connections. No-op for fakes used in tests. */
    fun close() {}
}
