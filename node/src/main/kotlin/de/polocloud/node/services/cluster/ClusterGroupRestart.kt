package de.polocloud.node.services.cluster

import de.polocloud.common.Address
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.communication.grpc.NodeGrpcClient
import de.polocloud.node.services.ServiceProvider
import de.polocloud.proto.NodeState
import de.polocloud.proto.RestartGroupServicesRequest
import de.polocloud.proto.ServiceManagerGrpcKt
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

/**
 * Restarts a group's running services in place (same group/index, fresh process) across
 * the whole cluster, not just the node that received the request — used by the terminal
 * `group <name> restart` command, so restarting a group with replicas spread over
 * several nodes doesn't require running the command once per node. Mirrors
 * [ClusterGroupShutdown].
 */
object ClusterGroupRestart {

    private val logger = LoggerFactory.getLogger(ClusterGroupRestart::class.java)

    /** Returns how many services, cluster-wide, were actually restarted. */
    suspend fun restartAcrossCluster(groupName: String, serviceProvider: ServiceProvider): Int {
        var restarted = serviceProvider.restartGroup(groupName)

        val peers = runCatching { NodeRepository.find(NodeState.ONLINE) }.getOrDefault(emptyList())
            .filter { it.id.toString() != serviceProvider.nodeId }
        if (peers.isEmpty()) return restarted

        val perPeer = coroutineScope {
            peers.map { node ->
                async {
                    // Isolate each peer: one that is down or slow must not block the rest
                    // of the cluster — same best-effort model as ClusterGroupShutdown.
                    runCatching { restartOnPeer(node, groupName) }
                        .onFailure { logger.warn("Failed to restart group '{}' on node {}: {}", groupName, node.name(), it.message) }
                        .getOrDefault(0)
                }
            }.awaitAll()
        }
        restarted += perPeer.sum()
        return restarted
    }

    private suspend fun restartOnPeer(node: NodeData, groupName: String): Int {
        val client = NodeGrpcClient()
        try {
            client.connect(Address(node.hostname, node.port))
            val stub = ServiceManagerGrpcKt.ServiceManagerCoroutineStub(client.channel())
            val request = RestartGroupServicesRequest.newBuilder().setGroupName(groupName).build()
            return stub.restartGroupServices(request).restartedCount
        } finally {
            client.disconnect()
        }
    }
}
