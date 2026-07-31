package de.polocloud.node.communication.handler.services

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.services.LocalService
import de.polocloud.node.services.Service
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.cluster.PeerServiceDataQuery
import de.polocloud.proto.NodeState
import de.polocloud.proto.ServiceData
import de.polocloud.proto.ServiceListRequest
import de.polocloud.shared.service.ServiceState
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class FindServicesAggregationTest {

    private val localNodeId = UUID.randomUUID()

    private fun providerWith(vararg names: Pair<String, Int>): ServiceProvider {
        val provider = ServiceProvider(nodeId = localNodeId.toString())
        names.forEach { (group, index) ->
            val local = LocalService(
                Service(UUID.randomUUID(), index, group, ServiceState.RUNNING, "10.0.0.1", 30000 + index)
            )
            provider.localServices += local
        }
        return provider
    }

    private fun node(id: UUID = UUID.randomUUID(), name: String = "node") =
        NodeData(id = id, nodeIndex = 1, groupName = name, hostname = "10.0.0.2", port = 4240, state = NodeState.ONLINE, version = "3", gitCommitHash = "abc")

    private fun peerService(group: String, index: Int, host: String) = ServiceData.newBuilder()
        .setId(UUID.randomUUID().toString()).setGroup(group).setIndex(index)
        .setState("RUNNING").setHost(host).setPort(30000 + index).build()

    private fun request(groupFilter: String? = null, localOnly: Boolean = false) =
        ServiceListRequest.newBuilder().apply {
            this.localOnly = localOnly
            groupFilter?.let { setGroupFilter(it) }
        }.build()

    @Test
    fun `single node returns only local services with their host`() = runBlocking {
        val handler = FindServicesServerHandler(providerWith("lobby" to 1), peers = { emptyList() })
        val response = handler.handle(request(), GrpcServerContext())
        assertEquals(listOf("lobby"), response.servicesList.map { it.group })
        assertEquals("10.0.0.1", response.servicesList.single().host)
    }

    @Test
    fun `local_only short-circuits and never queries peers`() = runBlocking {
        val queried = CopyOnWriteArrayList<String>()
        val handler = FindServicesServerHandler(
            providerWith("lobby" to 1),
            peers = { listOf(node(name = "node-b")) },
            peerQuery = { peer, _, _ -> queried += peer.name(); listOf(peerService("survival", 1, "10.0.0.2")) },
        )

        val response = handler.handle(request(localOnly = true), GrpcServerContext())

        assertEquals(listOf("lobby"), response.servicesList.map { it.group })
        assertTrue(queried.isEmpty())
    }

    @Test
    fun `aggregates local and peer services, preserving the peer host`() = runBlocking {
        val handler = FindServicesServerHandler(
            providerWith("lobby" to 1),
            peers = { listOf(node(name = "node-b")) },
            peerQuery = { _, _, _ -> listOf(peerService("survival", 1, "10.0.0.2")) },
        )

        val response = handler.handle(request(), GrpcServerContext())

        assertEquals(setOf("lobby", "survival"), response.servicesList.map { it.group }.toSet())
        val survival = response.servicesList.single { it.group == "survival" }
        assertEquals("10.0.0.2", survival.host) // proxy can reach the remote service
    }

    @Test
    fun `excludes the local node from the fan-out`() = runBlocking {
        val queried = CopyOnWriteArrayList<UUID>()
        val handler = FindServicesServerHandler(
            providerWith("lobby" to 1),
            peers = { listOf(node(id = localNodeId, name = "self"), node(name = "node-b")) },
            peerQuery = { peer, _, _ -> queried += peer.id; listOf(peerService("survival", 1, "10.0.0.2")) },
        )

        handler.handle(request(), GrpcServerContext())

        assertEquals(1, queried.size)
        assertTrue(localNodeId !in queried)
    }

    @Test
    fun `a failing peer is skipped, others still contribute`() = runBlocking {
        val handler = FindServicesServerHandler(
            providerWith("lobby" to 1),
            peers = { listOf(node(name = "bad"), node(name = "good")) },
            peerQuery = { peer, _, _ ->
                if (peer.name() == "bad-1") throw RuntimeException("boom")
                listOf(peerService("survival", 1, "10.0.0.2"))
            },
        )

        val response = handler.handle(request(), GrpcServerContext())
        assertEquals(setOf("lobby", "survival"), response.servicesList.map { it.group }.toSet())
    }

    @Test
    fun `group filter is applied to local services`() = runBlocking {
        val handler = FindServicesServerHandler(
            providerWith("lobby" to 1, "survival" to 1),
            peers = { emptyList() },
        )
        val response = handler.handle(request(groupFilter = "lobby"), GrpcServerContext())
        assertEquals(listOf("lobby"), response.servicesList.map { it.group })
    }
}
