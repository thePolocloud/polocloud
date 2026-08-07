package de.polocloud.node.communication.handler.services

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.cluster.NodePeerServiceCountQuery
import de.polocloud.node.services.cluster.PeerServiceCountQuery
import de.polocloud.proto.NodeState
import de.polocloud.proto.ServiceCountRequest
import de.polocloud.proto.ServiceCountResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

/**
 * Handles the API/SDK `CountServices` request.
 *
 * Cluster-wide, like [FindServicesServerHandler], but peers are asked for a plain count
 * ([PeerServiceCountQuery]) instead of their full local [de.polocloud.proto.ServiceData]
 * list — the whole point of a dedicated count RPC is to avoid transferring data that's
 * immediately discarded except for its size.
 */
class CountServicesServerHandler(
    private val serviceProvider: ServiceProvider,
    private val peers: () -> List<NodeData> = {
        runCatching { NodeRepository.find(NodeState.ONLINE) }.getOrDefault(emptyList())
    },
    private val peerQuery: PeerServiceCountQuery = NodePeerServiceCountQuery(),
) : GrpcServerHandler<ServiceCountRequest, ServiceCountResponse> {

    private val logger = LoggerFactory.getLogger(CountServicesServerHandler::class.java)

    override suspend fun handle(request: ServiceCountRequest, context: GrpcServerContext): ServiceCountResponse {
        val groupFilter = if (request.hasGroupFilter() && request.groupFilter.isNotBlank()) request.groupFilter else null
        val stateFilter = if (request.hasStateFilter() && request.stateFilter.isNotBlank()) request.stateFilter else null

        val local = localCount(groupFilter, stateFilter)
        val remote = if (request.localOnly) 0 else remoteCount(groupFilter, stateFilter)

        return ServiceCountResponse.newBuilder().setCount(local + remote).build()
    }

    private fun localCount(groupFilter: String?, stateFilter: String?): Int {
        var services = serviceProvider.localServices.toList().asSequence()
        if (groupFilter != null) services = services.filter { it.groupName.equals(groupFilter, ignoreCase = true) }
        if (stateFilter != null) services = services.filter { it.state.name.equals(stateFilter, ignoreCase = true) }
        return services.count()
    }

    private suspend fun remoteCount(groupFilter: String?, stateFilter: String?): Int {
        val others = peers().filter { it.id.toString() != serviceProvider.nodeId }
        if (others.isEmpty()) return 0

        return coroutineScope {
            others.map { node ->
                async {
                    runCatching { peerQuery.countLocalServicesOf(node, groupFilter, stateFilter) }
                        .onFailure { logger.warn("Failed to query service count from node {}: {}", node.name(), it.message) }
                        .getOrDefault(0)
                }
            }.awaitAll().sum()
        }
    }
}
