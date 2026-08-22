package de.polocloud.node.services.cluster

import de.polocloud.common.Address
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.communication.grpc.NodeGrpcClient
import de.polocloud.node.services.ServiceProvider
import de.polocloud.proto.ExecuteGroupServicesCommandRequest
import de.polocloud.proto.NodeState
import de.polocloud.proto.ServiceManagerGrpcKt
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

/**
 * Runs a console command in a group's running services across the whole cluster, not
 * just the node that received the request — used by the terminal `group <name> execute
 * <command>` command. Mirrors [ClusterGroupShutdown].
 */
object ClusterGroupExecute {

    private val logger = LoggerFactory.getLogger(ClusterGroupExecute::class.java)

    /** Returns how many services, cluster-wide, the command was actually sent to. */
    suspend fun executeAcrossCluster(groupName: String, command: String, serviceProvider: ServiceProvider): Int {
        var executed = serviceProvider.executeGroupCommand(groupName, command)

        val peers = runCatching { NodeRepository.find(NodeState.ONLINE) }.getOrDefault(emptyList())
            .filter { it.id.toString() != serviceProvider.nodeId }
        if (peers.isEmpty()) return executed

        val perPeer = coroutineScope {
            peers.map { node ->
                async {
                    // Isolate each peer: one that is down or slow must not block the rest
                    // of the cluster — same best-effort model as ClusterGroupShutdown.
                    runCatching { executeOnPeer(node, groupName, command) }
                        .onFailure { logger.warn("Failed to execute command on group '{}' on node {}: {}", groupName, node.name(), it.message) }
                        .getOrDefault(0)
                }
            }.awaitAll()
        }
        executed += perPeer.sum()
        return executed
    }

    private suspend fun executeOnPeer(node: NodeData, groupName: String, command: String): Int {
        val client = NodeGrpcClient()
        try {
            client.connect(Address(node.hostname, node.port))
            val stub = ServiceManagerGrpcKt.ServiceManagerCoroutineStub(client.channel())
            val request = ExecuteGroupServicesCommandRequest.newBuilder()
                .setGroupName(groupName)
                .setCommand(command)
                .build()
            return stub.executeGroupServicesCommand(request).executedCount
        } finally {
            client.disconnect()
        }
    }
}
