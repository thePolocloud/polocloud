package de.polocloud.shared.event

import de.polocloud.shared.event.group.GroupUpdatedEvent
import de.polocloud.shared.event.server.PlayerCountChangedEvent
import de.polocloud.shared.event.server.ServerStartedEvent
import de.polocloud.shared.event.server.ServerStoppedEvent
import de.polocloud.shared.property.Properties
import de.polocloud.shared.service.Service
import de.polocloud.shared.service.ServiceState
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EventSerializationTest {

    private val service = Service(
        id = "id-1", index = 1, group = "lobby", state = ServiceState.RUNNING,
        port = 30000, host = "127.0.0.1", pid = 99,
        properties = Properties().set(Properties.FALLBACK, "true"),
    )

    @Test
    fun `every shipped event is registered`() {
        assertNotNull(EventRegistry.classFor("ServerStartedEvent"))
        assertNotNull(EventRegistry.classFor("ServerStoppedEvent"))
        assertNotNull(EventRegistry.classFor("GroupUpdatedEvent"))
        assertNotNull(EventRegistry.classFor("PlayerCountChangedEvent"))
    }

    @Test
    fun `unknown event names resolve to null`() {
        assertNull(EventRegistry.classFor("NopeEvent"))
    }

    @Test
    fun `nameOf uses the simple class name`() {
        assertEquals("ServerStartedEvent", EventCodec.nameOf(ServerStartedEvent::class.java))
    }

    @Test
    fun `ServerStartedEvent round-trips through the codec`() {
        val encoded = EventCodec.encode(ServerStartedEvent(service))
        assertEquals("ServerStartedEvent", encoded.name)

        val decoded = EventCodec.decode(encoded.name, encoded.data)
        val event = assertInstanceOf(ServerStartedEvent::class.java, decoded)
        assertEquals(service, event.service)
        assertTrue(event.service.isFallback())
    }

    @Test
    fun `ServerStoppedEvent round-trips through the codec`() {
        val encoded = EventCodec.encode(ServerStoppedEvent(service))
        val decoded = EventCodec.decode(encoded.name, encoded.data)
        val event = assertInstanceOf(ServerStoppedEvent::class.java, decoded)
        assertEquals(service.name(), event.service.name())
    }

    @Test
    fun `PlayerCountChangedEvent round-trips with its player counts`() {
        val withPlayers = service.copy(onlinePlayers = 7, maxPlayers = 20)
        val encoded = EventCodec.encode(PlayerCountChangedEvent(withPlayers))
        assertEquals("PlayerCountChangedEvent", encoded.name)

        val decoded = EventCodec.decode(encoded.name, encoded.data)
        val event = assertInstanceOf(PlayerCountChangedEvent::class.java, decoded)
        assertEquals(7, event.service.onlinePlayers)
        assertEquals(20, event.service.maxPlayers)
    }

    @Test
    fun `GroupUpdatedEvent round-trips with its properties`() {
        val event = GroupUpdatedEvent("lobby", Properties().set(Properties.FALLBACK, "true").set("k", "v"))
        val encoded = EventCodec.encode(event)
        assertEquals("GroupUpdatedEvent", encoded.name)

        val decoded = EventCodec.decode(encoded.name, encoded.data)
        val result = assertInstanceOf(GroupUpdatedEvent::class.java, decoded)
        assertEquals("lobby", result.name)
        assertTrue(result.properties.getBoolean(Properties.FALLBACK))
        assertEquals("v", result.properties["k"])
    }

    @Test
    fun `decode returns null for an unregistered name`() {
        assertNull(EventCodec.decode("NopeEvent", "{}"))
    }

    @Test
    fun `encode throws for a non-serializable event`() {
        val bogus = object : Event {}
        assertThrows(SerializationException::class.java) {
            EventCodec.encode(bogus)
        }
    }

    @Test
    fun `custom events work without being added to EventRegistry`() {
        // CustomTestEvent is a plain @Serializable Event defined right in this test file,
        // never listed in EventRegistry's built-ins — standing in for a plugin's own event.
        val encoded = EventCodec.encode(CustomTestEvent("hello"))
        assertEquals("CustomTestEvent", encoded.name)

        // encode() registers the class as a side effect, so decode() resolves it here too —
        // mirroring a service that both fires and listens for its own custom event.
        val decoded = EventCodec.decode(encoded.name, encoded.data)
        val event = assertInstanceOf(CustomTestEvent::class.java, decoded)
        assertEquals("hello", event.payload)
    }
}

@Serializable
private data class CustomTestEvent(val payload: String) : Event
