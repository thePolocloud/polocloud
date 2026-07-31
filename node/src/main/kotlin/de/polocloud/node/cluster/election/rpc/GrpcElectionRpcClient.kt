package de.polocloud.node.cluster.election.rpc

import de.polocloud.common.Address
import de.polocloud.common.communication.tls.GrpcChannelFactory
import de.polocloud.common.communication.tls.MtlsConfig
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.security.NodeCertificateStorage
import de.polocloud.proto.LeaderHeartbeatRequest
import de.polocloud.proto.NodeServiceGrpcKt
import de.polocloud.proto.RequestVoteRequest
import io.grpc.ManagedChannel
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Real transport for [ElectionRpcClient]: unary calls to a peer's `NodeService`
 * (`RequestVote`/`LeaderHeartbeat`) over a cached mTLS channel — same caching and
 * drop-on-failure behavior as [de.polocloud.node.event.ClusterEventRelay], since
 * elections happen far more often than the TCP+TLS handshake should be repeated.
 */
class GrpcElectionRpcClient : ElectionRpcClient {

    private val logger = LoggerFactory.getLogger(GrpcElectionRpcClient::class.java)
    private val channels = ConcurrentHashMap<UUID, ManagedChannel>()

    override suspend fun requestVote(peer: NodeData, term: Long, candidateId: UUID): RequestVoteResult? {
        return runCatching {
            val stub = NodeServiceGrpcKt.NodeServiceCoroutineStub(channelFor(peer))
            val request = RequestVoteRequest.newBuilder()
                .setTerm(term)
                .setCandidateId(candidateId.toString())
                .build()
            val response = withTimeout(RPC_TIMEOUT_MILLIS) { stub.requestVote(request) }
            RequestVoteResult(response.term, response.voteGranted)
        }.onFailure { onCallFailed(peer, "RequestVote", it) }.getOrNull()
    }

    override suspend fun leaderHeartbeat(peer: NodeData, term: Long, leaderId: UUID): LeaderHeartbeatResult? {
        return runCatching {
            val stub = NodeServiceGrpcKt.NodeServiceCoroutineStub(channelFor(peer))
            val request = LeaderHeartbeatRequest.newBuilder()
                .setTerm(term)
                .setLeaderId(leaderId.toString())
                .build()
            val response = withTimeout(RPC_TIMEOUT_MILLIS) { stub.leaderHeartbeat(request) }
            LeaderHeartbeatResult(response.term, response.success)
        }.onFailure { onCallFailed(peer, "LeaderHeartbeat", it) }.getOrNull()
    }

    override fun close() {
        channels.values.forEach { runCatching { GrpcChannelFactory.shutdown(it) } }
        channels.clear()
    }

    private fun onCallFailed(peer: NodeData, rpc: String, ex: Throwable) {
        logger.warn("Election RPC '{}' to node {} failed: {}", rpc, peer.name(), ex.message)
        // Drop the cached channel so a stale connection (peer restarted, address
        // changed, cert rotated, ...) doesn't keep failing forever.
        channels.remove(peer.id)?.let { channel -> runCatching { GrpcChannelFactory.shutdown(channel) } }
    }

    private fun channelFor(node: NodeData): ManagedChannel = channels.computeIfAbsent(node.id) {
        val config = MtlsConfig.mutual(
            cert = NodeCertificateStorage.certificateFile(),
            key = NodeCertificateStorage.privateKeyFile(),
            caCert = NodeCertificateStorage.caCertificateFile(),
        )
        GrpcChannelFactory.secured(Address(node.hostname, node.port), config)
    }

    private companion object {
        const val RPC_TIMEOUT_MILLIS = 3_000L
    }
}
