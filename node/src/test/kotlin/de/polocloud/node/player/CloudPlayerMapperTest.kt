package de.polocloud.node.player

import de.polocloud.node.group.PropertyCodec
import de.polocloud.proto.PlayerData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Covers [CloudPlayerMapper]'s proto <-> node <-> shared round trip — mirrors
 * `node/src/test/kotlin/de/polocloud/node/services/ServiceMappersTest.kt`.
 */
class CloudPlayerMapperTest {

    private fun player(currentServer: String? = "lobby-1") = CloudPlayer(
        id = UUID.randomUUID(),
        name = "Notch",
        skinValue = "texture-value",
        skinSignature = "texture-signature",
        propertiesJson = PropertyCodec.encode(mapOf("textures" to "texture-value")),
        currentProxy = "proxy-1",
        currentServer = currentServer,
    )

    @Test
    fun `toProto and toDomain round-trip a player`() {
        val original = player()

        val proto = CloudPlayerMapper.toProto(original)
        val restored = CloudPlayerMapper.toDomain(proto)

        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.skinValue, restored.skinValue)
        assertEquals(original.skinSignature, restored.skinSignature)
        assertEquals(original.properties, restored.properties)
        assertEquals(original.currentProxy, restored.currentProxy)
        assertEquals(original.currentServer, restored.currentServer)
    }

    @Test
    fun `toProto encodes a null currentServer as an empty string, toDomain decodes it back to null`() {
        val original = player(currentServer = null)

        val proto = CloudPlayerMapper.toProto(original)
        assertEquals("", proto.currentServer)

        val restored = CloudPlayerMapper.toDomain(proto)
        assertNull(restored.currentServer)
    }

    @Test
    fun `toShared carries every field over to the wire model`() {
        val original = player()

        val shared = CloudPlayerMapper.toShared(original)

        assertEquals(original.id.toString(), shared.id)
        assertEquals(original.name, shared.name)
        assertEquals(original.skinValue, shared.skinValue)
        assertEquals(original.skinSignature, shared.skinSignature)
        assertEquals(original.properties, shared.properties.asMap())
        assertEquals(original.currentProxy, shared.currentProxy)
        assertEquals(original.currentServer, shared.currentServer)
    }

    @Test
    fun `toDomain rejects a player id that is not a valid UUID`() {
        val data = PlayerData.newBuilder().setId("not-a-uuid").setName("Notch").build()

        assertThrows(IllegalArgumentException::class.java) {
            CloudPlayerMapper.toDomain(data)
        }
    }
}
