package de.polocloud.node.communication.handler.services

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.services.LocalService
import de.polocloud.node.services.Service
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.cluster.PeerServiceQuery
import de.polocloud.proto.ListServicesRequest
import de.polocloud.proto.NodeState
import de.polocloud.proto.ProtoServiceProcessData
import de.polocloud.shared.service.ServiceState
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class ListServicesAggregationTest {

    private val localNodeId = UUID.randomUUID()

    private fun providerWith(vararg names: Pair<String, Int>): ServiceProvider {
        val provider = ServiceProvider(nodeId = localNodeId.toString())
        names.forEach { (group, index) ->
            provider.localServices += LocalService(
                Service(UUID.randomUUID(), index, group, ServiceState.RUNNING, "127.0.0.1", 30000 + index)
            )
        }
        return provider
    }

    private fun node(id: UUID = UUID.randomUUID(), name: String = "node") =
        NodeData(id = id, nodeIndex = 1, groupName = name, hostname = "10.0.0.1", port = 4240, state = NodeState.ONLINE, version = "3", gitCommitHash = "abc")

    private fun peerService(group: String, index: Int) = ProtoServiceProcessData.newBuilder()
        .setUuid(UUID.randomUUID().toString()).setPlan(group).setIndex(index).setState("RUNNING").build()

    private fun request(planName: String = "", localOnly: Boolean = false) =
        ListServicesRequest.newBuilder().setPlanName(planName).setLocalOnly(localOnly).build()

    @Test
    fun `single node returns only local services`() = runBlocking {
        val handler = ListServicesServerHandler(providerWith("lobby" to 1), peers = { emptyList() })
        val response = handler.handle(request(), GrpcServerContext())
        assertEquals(listOf("lobby"), response.serviceProcessList.map { it.plan })
    }

    @Test
    fun `local_only short-circuits and never queries peers`() = runBlocking {
        val queried = CopyOnWriteArrayList<String>()
        val handler = ListServicesServerHandler(
            providerWith("lobby" to 1),
            peers = { listOf(node(name = "node-b")) },
            peerQuery = { peer, _ -> queried += peer.name(); listOf(peerService("survival", 1)) },
        )

        val response = handler.handle(request(localOnly = true), GrpcServerContext())

        assertEquals(listOf("lobby"), response.serviceProcessList.map { it.plan })
        assertTrue(queried.isEmpty(), "peers must not be queried for a local_only request")
    }

    @Test
    fun `aggregates local services with peer services`() = runBlocking {
        val peer = node(name = "node-b")
        val handler = ListServicesServerHandler(
            providerWith("lobby" to 1),
            peers = { listOf(peer) },
            peerQuery = { _, _ -> listOf(peerService("survival", 1), peerService("survival", 2)) },
        )

        val response = handler.handle(request(), GrpcServerContext())

        assertEquals(setOf("lobby", "survival"), response.serviceProcessList.map { it.plan }.toSet())
        assertEquals(3, response.serviceProcessList.size)
    }

    @Test
    fun `excludes the local node from the peer fan-out`() = runBlocking {
        val queried = CopyOnWriteArrayList<UUID>()
        val handler = ListServicesServerHandler(
            providerWith("lobby" to 1),
            // The provider itself appears in the node list (same id) and must be skipped.
            peers = { listOf(node(id = localNodeId, name = "self"), node(name = "node-b")) },
            peerQuery = { peer, _ -> queried += peer.id; listOf(peerService("survival", 1)) },
        )

        handler.handle(request(), GrpcServerContext())

        assertEquals(1, queried.size)
        assertTrue(localNodeId !in queried)
    }

    @Test
    fun `a failing peer is skipped, others still contribute`() = runBlocking {
        val good = node(name = "good")
        val bad = node(name = "bad")
        val handler = ListServicesServerHandler(
            providerWith("lobby" to 1),
            peers = { listOf(bad, good) },
            peerQuery = { peer, _ ->
                if (peer.name() == "bad-1") throw RuntimeException("boom")
                listOf(peerService("survival", 1))
            },
        )

        val response = handler.handle(request(), GrpcServerContext())

        // lobby (local) + survival (good peer); the bad peer contributes nothing.
        assertEquals(setOf("lobby", "survival"), response.serviceProcessList.map { it.plan }.toSet())
    }

    @Test
    fun `plan filter is applied to local services`() = runBlocking {
        val handler = ListServicesServerHandler(
            providerWith("lobby" to 1, "survival" to 1),
            peers = { emptyList() },
        )
        val response = handler.handle(request(planName = "lobby"), GrpcServerContext())
        assertEquals(listOf("lobby"), response.serviceProcessList.map { it.plan })
    }
}
