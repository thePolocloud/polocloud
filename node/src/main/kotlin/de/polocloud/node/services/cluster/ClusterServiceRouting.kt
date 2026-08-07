package de.polocloud.node.services.cluster

import de.polocloud.common.Address
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.communication.grpc.NodeGrpcClient
import de.polocloud.node.services.Service
import org.slf4j.Logger
import java.util.UUID

/**
 * Resolves which node actually hosts a service, for handlers that need to forward a
 * request there (see [de.polocloud.node.communication.handler.services.StopServiceServerHandler],
 * [de.polocloud.node.communication.handler.services.ExecuteServiceCommandServerHandler]).
 */
object ClusterServiceRouting {

    /** The node hosting [service], or `null` if that's [selfNodeId] itself or can't be resolved. */
    fun resolveOwningNode(service: Service, selfNodeId: String): NodeData? {
        if (service.nodeId.isBlank() || service.nodeId == selfNodeId) return null
        val id = runCatching { UUID.fromString(service.nodeId) }.getOrNull() ?: return null
        return NodeRepository.find(id)
    }

    /**
     * Forwards a request to [node] over a fresh mTLS [NodeGrpcClient] connection: connects,
     * runs [call] (which builds the peer's gRPC stub and invokes the actual RPC on it) and
     * disconnects again once [call] returns or throws — regardless of which. A connection
     * or RPC failure is logged through [logger] and turned into a handler-specific failure
     * response via [onFailure] instead of propagating, since a forwarding handler always
     * owes its own caller *a* response rather than a thrown exception.
     *
     * Shared by the otherwise near-identical forwarding tail of
     * [de.polocloud.node.communication.handler.services.StopServiceServerHandler],
     * [de.polocloud.node.communication.handler.services.ExecuteServiceCommandServerHandler]
     * and [de.polocloud.node.communication.handler.services.CopyTemplateServerHandler] — each
     * keeps its own request/response types and passes them through [call]/[onFailure].
     */
    suspend fun <Response> forwardToOwningNode(
        node: NodeData,
        logger: Logger,
        requestName: String,
        serviceName: String,
        onFailure: (message: String) -> Response,
        call: suspend (NodeGrpcClient) -> Response,
    ): Response {
        val client = NodeGrpcClient()
        return try {
            client.connect(Address(node.hostname, node.port))
            call(client)
        } catch (ex: Exception) {
            logger.warn("Failed to forward {} for '{}' to node {}: {}", requestName, serviceName, node.name(), ex.message)
            onFailure("Could not reach the node hosting '$serviceName': ${ex.message}")
        } finally {
            client.disconnect()
        }
    }
}
