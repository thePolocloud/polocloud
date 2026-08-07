package de.polocloud.node.services.cluster

import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.services.Service
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
}
