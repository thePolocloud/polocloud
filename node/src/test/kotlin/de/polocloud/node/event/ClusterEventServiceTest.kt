package de.polocloud.node.event

import de.polocloud.proto.EventContext
import de.polocloud.shared.event.EncodedEvent
import de.polocloud.shared.event.group.GroupUpdatedEvent
import de.polocloud.shared.property.Properties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList

class ClusterEventServiceTest {

    @AfterEach
    fun reset() {
        ClusterEventService.peerRelay = null
    }

    @Test
    fun `call forwards locally-fired events to the peer relay`() {
        val relayed = CopyOnWriteArrayList<EncodedEvent>()
        ClusterEventService.peerRelay = { relayed += it }

        ClusterEventService.call(GroupUpdatedEvent("lobby", Properties().set(Properties.FALLBACK, "true")))

        assertEquals(1, relayed.size)
        assertEquals("GroupUpdatedEvent", relayed.single().name)
    }

    @Test
    fun `publish forwards SDK-originated events to the peer relay`() {
        val relayed = CopyOnWriteArrayList<EncodedEvent>()
        ClusterEventService.peerRelay = { relayed += it }

        ClusterEventService.publish(
            EventContext.newBuilder().setEventName("ServerStartEvent").setEventData("{}").build()
        )

        assertEquals(listOf("ServerStartEvent"), relayed.map { it.name })
    }

    @Test
    fun `broadcast does not hit the peer relay (loop guard for relayed-in events)`() {
        val relayed = CopyOnWriteArrayList<EncodedEvent>()
        ClusterEventService.peerRelay = { relayed += it }

        // broadcast is the path used when re-delivering an event received from a peer;
        // it must never bounce the event back out to peers.
        ClusterEventService.broadcast(
            EventContext.newBuilder().setEventName("GroupUpdatedEvent").setEventData("{}").build()
        )

        assertTrue(relayed.isEmpty())
    }

    @Test
    fun `a failing peer relay never breaks local delivery`() {
        ClusterEventService.peerRelay = { throw RuntimeException("relay down") }
        // Must not throw — call swallows relay failures.
        ClusterEventService.call(GroupUpdatedEvent("lobby", Properties()))
    }
}
