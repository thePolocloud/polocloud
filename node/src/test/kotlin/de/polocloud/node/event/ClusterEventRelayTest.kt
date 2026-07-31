package de.polocloud.node.event

import de.polocloud.node.cluster.node.NodeData
import de.polocloud.proto.NodeState
import de.polocloud.shared.event.EncodedEvent
import de.polocloud.shared.event.group.GroupUpdatedEvent
import de.polocloud.shared.property.Properties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ClusterEventRelayTest {

    private val localNodeId = UUID.randomUUID()

    @AfterEach
    fun reset() {
        ClusterEventService.peerRelay = null
    }

    private fun node(id: UUID = UUID.randomUUID(), name: String = "node") =
        NodeData(id = id, nodeIndex = 1, groupName = name, hostname = "10.0.0.9", port = 4240, state = NodeState.ONLINE, version = "3", gitCommitHash = "abc")

    private fun fire() = ClusterEventService.call(GroupUpdatedEvent("lobby", Properties()))

    @Test
    fun `install routes fired events to every peer except self`() {
        val sentTo = CopyOnWriteArrayList<UUID>()
        val latch = CountDownLatch(2)
        val peerB = node(name = "b")
        val peerC = node(name = "c")

        val relay = ClusterEventRelay(
            localNodeId = localNodeId.toString(),
            peers = { listOf(node(id = localNodeId, name = "self"), peerB, peerC) },
            send = { peer, _ -> sentTo += peer.id; latch.countDown() },
        )
        relay.install()

        fire()

        assertTrue(latch.await(2, TimeUnit.SECONDS))
        assertEquals(setOf(peerB.id, peerC.id), sentTo.toSet())
        assertTrue(localNodeId !in sentTo)
        relay.close()
    }

    @Test
    fun `the relayed event carries the encoded payload`() {
        val received = CopyOnWriteArrayList<EncodedEvent>()
        val latch = CountDownLatch(1)
        val relay = ClusterEventRelay(
            localNodeId = localNodeId.toString(),
            peers = { listOf(node(name = "b")) },
            send = { _, encoded -> received += encoded; latch.countDown() },
        )
        relay.install()

        fire()

        assertTrue(latch.await(2, TimeUnit.SECONDS))
        assertEquals("GroupUpdatedEvent", received.single().name)
        relay.close()
    }

    @Test
    fun `a failing peer send is isolated from the others`() {
        val delivered = CopyOnWriteArrayList<String>()
        val latch = CountDownLatch(2)
        val relay = ClusterEventRelay(
            localNodeId = localNodeId.toString(),
            peers = { listOf(node(name = "bad"), node(name = "good")) },
            send = { peer, _ ->
                try {
                    if (peer.name() == "bad-1") throw RuntimeException("boom")
                    delivered += peer.name()
                } finally {
                    latch.countDown()
                }
            },
        )
        relay.install()

        fire()

        assertTrue(latch.await(2, TimeUnit.SECONDS))
        assertEquals(listOf("good-1"), delivered.toList())
        relay.close()
    }

    @Test
    fun `close removes the relay hook`() {
        val relay = ClusterEventRelay(localNodeId.toString(), peers = { emptyList() }, send = { _, _ -> })
        relay.install()
        relay.close()
        assertNull(ClusterEventService.peerRelay)
    }

    @Test
    fun `single node relay is a no-op`() {
        val sent = CopyOnWriteArrayList<UUID>()
        val relay = ClusterEventRelay(
            localNodeId = localNodeId.toString(),
            peers = { listOf(node(id = localNodeId, name = "self")) }, // only self online
            send = { peer, _ -> sent += peer.id },
        )
        relay.install()
        fire()
        // Nothing to fan out to; give any stray async task a moment, then assert none ran.
        Thread.sleep(100)
        assertTrue(sent.isEmpty())
        relay.close()
    }
}
