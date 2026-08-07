package de.polocloud.node.services.cluster

import de.polocloud.common.Address
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.communication.grpc.NodeGrpcClient
import de.polocloud.proto.ServiceApiServiceGrpcKt
import de.polocloud.proto.ServiceCountRequest
import kotlinx.coroutines.withTimeout

/**
 * Fetches the *local* service count of a single peer node — the count-only counterpart
 * of [PeerServiceDataQuery], used so a cluster-wide count doesn't have to transfer every
 * peer's full [de.polocloud.proto.ServiceData] list just to measure its size.
 *
 * Kept behind an interface so the aggregation logic can be unit-tested without a real
 * gRPC channel.
 */
fun interface PeerServiceCountQuery {

    /** Returns the number of services running locally on [node], filtered by [groupFilter]/[stateFilter]. */
    suspend fun countLocalServicesOf(node: NodeData, groupFilter: String?, stateFilter: String?): Int
}

/** Real [PeerServiceCountQuery]: opens a short-lived mTLS channel to the peer's node endpoint. */
class NodePeerServiceCountQuery(
    private val timeoutMillis: Long = 3_000,
) : PeerServiceCountQuery {

    override suspend fun countLocalServicesOf(
        node: NodeData,
        groupFilter: String?,
        stateFilter: String?,
    ): Int {
        val client = NodeGrpcClient()
        return try {
            client.connect(Address(node.hostname, node.port))
            val stub = ServiceApiServiceGrpcKt.ServiceApiServiceCoroutineStub(client.channel())
            val request = ServiceCountRequest.newBuilder().apply {
                localOnly = true
                groupFilter?.let { setGroupFilter(it) }
                stateFilter?.let { setStateFilter(it) }
            }.build()
            withTimeout(timeoutMillis) { stub.countServices(request).count }
        } finally {
            client.disconnect()
        }
    }
}
