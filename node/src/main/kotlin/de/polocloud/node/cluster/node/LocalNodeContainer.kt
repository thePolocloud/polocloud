package de.polocloud.node.cluster.node

import de.polocloud.proto.NodeState
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class LocalNodeContainer(val data: NodeData) : NodeContainer(data) {

    fun markOnline() =
        changeState(NodeState.ONLINE) {
            it == NodeState.STARTING || it == NodeState.SYNCING
        }

    fun markStarting() =
        changeState(NodeState.STARTING) {
           true
        }

    fun markStopping() =
        changeState(NodeState.STOPPING) {
            it == NodeState.ONLINE || it == NodeState.CRASHED || it == NodeState.STARTING
        }

    fun markStopped() =
        changeState(NodeState.STOPPED) {
            it == NodeState.STOPPING || it == NodeState.CRASHED
        }


    @OptIn(ExperimentalAtomicApi::class)
    private fun changeState(
        newState: NodeState,
        predicate: (NodeState) -> Boolean
    ) {
        if (!predicate(this.state())) {
            return
        }

        data.state = newState
        NodeRepository.save(this.data)
    }

    /**
     * Adopts the identity a live `cluster join` (see
     * [de.polocloud.node.identity.NodeIdentityService.joinLive]) received from the
     * accepting cluster into this existing, already-referenced [data] object, then
     * persists it — rather than replacing [data]/this container outright, which would
     * leave any other component holding a direct reference to the old objects stale.
     *
     * [newData] must describe the same node ([NodeData.id] unchanged — a join never
     * changes this node's own identity, only which cluster it belongs to); only the
     * fields the accepting cluster actually (re-)assigns on registration are copied —
     * see `RegistrationService.registerNode`'s field-by-field behavior.
     */
    fun adoptJoinedIdentity(newData: NodeData) {
        require(newData.id == data.id) { "adoptJoinedIdentity must not change this node's own id (was ${data.id}, got ${newData.id})" }

        data.nodeIndex = newData.nodeIndex
        data.state = newData.state
        data.head = newData.head
        data.electedAt = newData.electedAt
        data.term = newData.term
        data.votedFor = newData.votedFor
        data.firstConnection = newData.firstConnection
        data.lastConnection = newData.lastConnection

        NodeRepository.save(this.data)
    }
}