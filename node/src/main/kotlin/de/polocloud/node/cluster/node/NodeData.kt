package de.polocloud.node.cluster.node

import de.polocloud.database.EntryIdentifier
import de.polocloud.database.RepositoryName
import de.polocloud.proto.NodeState
import de.polocloud.proto.ProtoNodeData
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant

@RepositoryName("nodes")
// No default values on this constructor, even ergonomic ones — SqlExecutor.resolveMeta()
// in polocloud-database picks `declaredConstructors.first()` to reflectively rebuild
// rows, and a Kotlin default parameter value makes the compiler emit a second, synthetic
// constructor (real-arg-count + 2 for the bitmask/marker). `declaredConstructors` order
// is unspecified, so `.first()` can pick either one — when it picks the synthetic one,
// SqlExecutor.mapRow()'s N-arg call throws and every find()/findAll() on this table
// silently returns empty (the exception is swallowed and logged), which in turn breaks
// e.g. CreateTokenServerHandler's known-node check. Construct a fresh node via
// [de.polocloud.node.cluster.node.NodeFactory], which supplies the same values the
// removed defaults used to.
data class NodeData(
    @EntryIdentifier val id : UUID,
    val nodeIndex: Int,
    val groupName: String,
    val hostname: String,
    val port: Int,
    var state: NodeState,
    var head: Boolean,
    var electedAt: Instant?,
    /** Highest election term this node has observed. Persisted so a restart can't cause it to vote twice in the same term. */
    var term: Long,
    /** Candidate this node voted for in [term]. Reset whenever [term] advances. */
    var votedFor: UUID?,
    val version: String,
    val gitCommitHash: String,
    val firstConnection: Instant,
    var lastConnection: Instant,
    /**
     * Total system memory (MB) this node reported at registration. 0 means unknown/not
     * reported (e.g. a node registered before this field existed) and is treated as
     * unlimited capacity by [de.polocloud.node.services.queue.ServiceQueue], never as
     * zero capacity.
     */
    val maxMemory: Int,
) {

    fun name(): String {
        return "$groupName-$nodeIndex"
    }
}

fun NodeData.toProto(): ProtoNodeData {
    return ProtoNodeData.newBuilder()
        .setId(this.id.toString())
        .setIndex(this.nodeIndex)
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