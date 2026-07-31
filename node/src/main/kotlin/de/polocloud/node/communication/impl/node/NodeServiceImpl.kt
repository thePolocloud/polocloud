package de.polocloud.node.communication.impl.node

import de.polocloud.common.communication.server.executor.GrpcServerExecutor
import de.polocloud.node.cluster.election.NodeElectionService
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.communication.grpc.GrpcContextFactory
import de.polocloud.node.communication.interceptor.CliSessionInterceptor
import de.polocloud.node.event.ClusterEventService
import de.polocloud.node.security.NodeCertificateStorage
import de.polocloud.node.services.ServiceProvider
import de.polocloud.proto.EventContext
import de.polocloud.proto.FetchClusterCaRequest
import de.polocloud.proto.FetchClusterCaResponse
import de.polocloud.proto.FetchForwardingSecretRequest
import de.polocloud.proto.FetchForwardingSecretResponse
import de.polocloud.proto.LeaderHeartbeatRequest
import de.polocloud.proto.LeaderHeartbeatResponse
import de.polocloud.proto.NodeEvent
import de.polocloud.proto.NodeEventRequest
import de.polocloud.proto.NodeInformationRequest
import de.polocloud.proto.NodeInformationResponse
import de.polocloud.proto.NodeServiceGrpcKt
import de.polocloud.proto.RelayEventRequest
import de.polocloud.proto.RelayEventResponse
import de.polocloud.proto.RequestVoteRequest
import de.polocloud.proto.RequestVoteResponse
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class NodeServiceImpl(
    private val executor: GrpcServerExecutor,
    private val serviceProvider: ServiceProvider,
    private val electionService: NodeElectionService,
) : NodeServiceGrpcKt.NodeServiceCoroutineImplBase() {

    private val listeners = mutableSetOf<SendChannel<NodeEvent>>()

    override suspend fun getNodeInformation(request: NodeInformationRequest): NodeInformationResponse {
        return executor.execute(request, GrpcContextFactory.fromGrpc())
    }

    override fun listenForEvents(request: NodeEventRequest): Flow<NodeEvent> = callbackFlow {
        listeners += channel

        awaitClose { listeners -= channel }
    }

    /**
     * Receives an event relayed from a peer node and re-broadcasts it to this node's
     * local subscribers only (via [ClusterEventService.broadcast], which does not relay
     * again) — so a peer's event reaches local bridges/SDKs without creating a loop.
     */
    override suspend fun relayEvent(request: RelayEventRequest): RelayEventResponse {
        ClusterEventService.broadcast(
            EventContext.newBuilder()
                .setEventName(request.eventName)
                .setEventData(request.eventData)
                .build()
        )
        return RelayEventResponse.newBuilder().setSuccess(true).build()
    }

    /**
     * Hands the real cluster CA key pair to a peer node so it can safely be promoted to
     * head later — see [NodeCertificateStorage.adoptClusterCaKeyPair].
     *
     * Restricted to callers whose peer certificate CN resolves to a known node id: the
     * mTLS port here also serves CLI and service clients trusted by the same CA (see
     * [de.polocloud.node.communication.grpc.NodeGrpcEndpoint]'s doc comment), and neither
     * of those should ever be able to obtain the CA private key just by asking for it.
     */
    override suspend fun fetchClusterCa(request: FetchClusterCaRequest): FetchClusterCaResponse {
        val callerId = runCatching { UUID.fromString(CliSessionInterceptor.SUBJECT_CTX_KEY.get()) }.getOrNull()
        if (callerId == null || NodeRepository.find(callerId) == null) {
            return FetchClusterCaResponse.newBuilder()
                .setAvailable(false)
                .setMessage("Caller is not a known node identity")
                .build()
        }

        val ca = NodeCertificateStorage.certificateAuthority()
        return FetchClusterCaResponse.newBuilder()
            .setAvailable(true)
            .setCaPrivateKey(ca.getCaPrivateKeyPem())
            .setCaPublicKey(ca.getCaPublicKeyPem())
            .build()
    }

    /**
     * Hands this node's forwarding secret to a peer that just joined the cluster, so the
     * services it starts (proxy or backend) agree with everyone else's — see
     * [de.polocloud.node.forwarding.ForwardingHandler.adopt].
     *
     * Restricted the same way as [fetchClusterCa]: only callers whose peer certificate CN
     * resolves to a known node id.
     */
    override suspend fun fetchForwardingSecret(request: FetchForwardingSecretRequest): FetchForwardingSecretResponse {
        val callerId = runCatching { UUID.fromString(CliSessionInterceptor.SUBJECT_CTX_KEY.get()) }.getOrNull()
        if (callerId == null || NodeRepository.find(callerId) == null) {
            return FetchForwardingSecretResponse.newBuilder()
                .setAvailable(false)
                .setMessage("Caller is not a known node identity")
                .build()
        }

        return FetchForwardingSecretResponse.newBuilder()
            .setAvailable(true)
            .setSecret(serviceProvider.forwardingHandler.secret)
            .build()
    }

    /**
     * Restricted the same way as [fetchClusterCa]/[fetchForwardingSecret]: only callers
     * whose peer certificate CN resolves to a known node id may participate in an
     * election — an unauthenticated/unknown caller casting a "vote" could otherwise
     * skew majority counting. Additionally, [RequestVoteRequest.candidateId] must equal
     * the authenticated caller's own id: nothing about the mTLS handshake otherwise stops
     * an admitted-but-dishonest node from claiming candidacy on behalf of a *different*
     * node id in the payload, which would let it manipulate that other node's `votedFor`
     * without it actually running for election.
     */
    override suspend fun requestVote(request: RequestVoteRequest): RequestVoteResponse {
        val callerId = runCatching { UUID.fromString(CliSessionInterceptor.SUBJECT_CTX_KEY.get()) }.getOrNull()
        val candidateId = runCatching { UUID.fromString(request.candidateId) }.getOrNull()
        if (callerId == null || candidateId != callerId || NodeRepository.find(callerId) == null) {
            return RequestVoteResponse.newBuilder().setTerm(request.term).setVoteGranted(false).build()
        }

        val result = electionService.handleRequestVote(request.term, candidateId)
        return RequestVoteResponse.newBuilder()
            .setTerm(result.term)
            .setVoteGranted(result.voteGranted)
            .build()
    }

    /**
     * Restricted the same way as [requestVote]: [LeaderHeartbeatRequest.leaderId] must
     * equal the authenticated caller's own id, otherwise any admitted node could forge a
     * heartbeat claiming an arbitrary (or even non-member) node as leader and have it
     * projected onto [NodeRepository.head] by [electionService][NodeElectionService].
     */
    override suspend fun leaderHeartbeat(request: LeaderHeartbeatRequest): LeaderHeartbeatResponse {
        val callerId = runCatching { UUID.fromString(CliSessionInterceptor.SUBJECT_CTX_KEY.get()) }.getOrNull()
        val leaderId = runCatching { UUID.fromString(request.leaderId) }.getOrNull()
        if (callerId == null || leaderId != callerId || NodeRepository.find(callerId) == null) {
            return LeaderHeartbeatResponse.newBuilder().setTerm(request.term).setSuccess(false).build()
        }

        val result = electionService.handleLeaderHeartbeat(request.term, leaderId)
        return LeaderHeartbeatResponse.newBuilder()
            .setTerm(result.term)
            .setSuccess(result.success)
            .build()
    }

    fun broadcastShutdown() {
        val event = NodeEvent.newBuilder()
            .setType(NodeEvent.Type.NODE_SHUTDOWN)
            .build()

        listeners.forEach { it.trySend(event) }
    }
}