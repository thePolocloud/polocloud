package de.polocloud.node.cluster.node

import de.polocloud.database.EntryIdentifier
import de.polocloud.database.RepositoryName
import de.polocloud.proto.NodeState
import de.polocloud.proto.ProtoNodeData
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant

@RepositoryName("nodes")
data class NodeData(
    @EntryIdentifier val id : UUID,
    val index: Int,
    val groupName: String = "node",
    val hostname: String,
    val port: Int,
    var state: NodeState,
    var head: Boolean = false,
    var electedAt: Instant? = null,
    /** Highest election term this node has observed. Persisted so a restart can't cause it to vote twice in the same term. */
    var term: Long = 0,
    /** Candidate this node voted for in [term]. Reset whenever [term] advances. */
    var votedFor: UUID? = null,
    val version: String,
    val gitCommitHash: String,
    val firstConnection: Instant = Clock.System.now(),
    var lastConnection: Instant = Clock.System.now(),
    /**
     * Total system memory (MB) this node reported at registration. 0 means unknown/not
     * reported (e.g. a node registered before this field existed) and is treated as
     * unlimited capacity by [de.polocloud.node.services.queue.ServiceQueue], never as
     * zero capacity.
     */
    val maxMemory: Int = 0,
) {

    fun name(): String {
        return "$groupName-$index"
    }
}

fun NodeData.toProto(): ProtoNodeData {
    return ProtoNodeData.newBuilder()
        .setId(this.id.toString())
        .setIndex(this.index)
        .setGroupName(this.groupName)
        .setHostname(this.hostname)
        .setPort(this.port)
        .setState(this.state)
        .setHead(this.head)
        .setVersion(this.version)
        .setGitCommitHash(this.gitCommitHash)
        .setFirstConnection(this.firstConnection.toEpochMilliseconds())
        .setLastConnection(this.lastConnection.toEpochMilliseconds())
        .setMaxMemory(this.maxMemory)
        .build()
}