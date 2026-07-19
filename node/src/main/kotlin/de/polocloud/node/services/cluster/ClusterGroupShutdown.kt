package de.polocloud.node.services.cluster

import de.polocloud.common.Address
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.communication.grpc.NodeGrpcClient
import de.polocloud.node.services.ServiceProvider
import de.polocloud.proto.NodeState
import de.polocloud.proto.ServiceManagerGrpcKt
import de.polocloud.proto.StopGroupServicesRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

/**
 * Stops a group's running/queued services across the whole cluster, not just the node
 * that received the delete request — used by both the terminal `group delete` command
 * and `GroupApiService.DeleteGroup` right before the group row itself is deleted.
 *
 * Without this, only the receiving node's own services were ever stopped: replicas
 * placed on other nodes kept running as orphaned processes, and — since a queued or
 * running service is a DB row referencing the group — deleting the group failed on the
 * groups table's foreign-key constraint whenever any node still had a row for it.
 */
object ClusterGroupShutdown {

    private val logger = LoggerFactory.getLogger(ClusterGroupShutdown::class.java)

    suspend fun shutdownAcrossCluster(groupName: String, serviceProvider: ServiceProvider) {
        serviceProvider.shutdownGroup(groupName)

        val peers = runCatching { NodeRepository.find(NodeState.ONLINE) }.getOrDefault(emptyList())
            .filter { it.id.toString() != serviceProvider.nodeId }
        if (peers.isEmpty()) return

        coroutineScope {
            peers.map { node ->
                async {
                    // Isolate each peer: one that is down or slow must not block the group
                    // delete on the rest of the cluster — same best-effort model as the
                    // cluster-wide service listing (see NodePeerServiceQuery).
                    runCatching { stopOnPeer(node, groupName) }
                        .onFailure { logger.warn("Failed to stop group '{}' on node {}: {}", groupName, node.name(), it.message) }
                }
            }.awaitAll()
        }
    }

    private suspend fun stopOnPeer(node: NodeData, groupName: String) {
        val client = NodeGrpcClient()
        try {
            client.connect(Address(node.hostname, node.port))
            val stub = ServiceManagerGrpcKt.ServiceManagerCoroutineStub(client.channel())
            stub.stopGroupServices(StopGroupServicesRequest.newBuilder().setGroupName(groupName).build())
        } finally {
            client.disconnect()
        }
    }
}