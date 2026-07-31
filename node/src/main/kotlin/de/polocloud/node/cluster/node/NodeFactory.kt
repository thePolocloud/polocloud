package de.polocloud.node.cluster.node

import de.polocloud.common.Address
import de.polocloud.common.os.SystemResources
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.node.core.environment.NodeEnvironment
import de.polocloud.proto.NodeState
import java.util.*
import kotlin.time.Clock.System.now
import kotlin.time.Instant

object NodeFactory {

    fun createInitial(address: Address, group: String): NodeData =
        create(
            NodeEnvironment.runtime.nodeId.get(), 1, group, address,
            PolocloudVersion.CURRENT.toVersionString(), PolocloudVersion.CURRENT.commitId,
            true, now(), SystemResources.maxMemory().toInt(),
        )

    fun create(
        id: UUID,
        index: Int,
        groupName: String,
        address: Address,
        version: String,
        gitCommitHash: String,
        head: Boolean = false,
        electedAt: Instant? = null,
        maxMemory: Int = 0,
    ): NodeData =
        NodeData(
            id = id,
            nodeIndex = index,
            groupName = groupName,
            hostname = address.hostname,
            port = address.port,
            state = NodeState.STARTING,
            head = head,
            electedAt = electedAt,
            version = version,
            gitCommitHash = gitCommitHash,
            maxMemory = maxMemory,
        )
}