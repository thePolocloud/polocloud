package de.polocloud.node.group

import de.polocloud.proto.GroupData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Guards the persistence-safe design of group properties.
 *
 * The SQL layer maps every declared field to its own column and cannot persist a
 * `Map`, so [Group] stores properties as the JSON string [Group.propertiesJson] and
 * exposes them through a computed [Group.properties] getter. If that getter ever
 * grew a backing field it would silently become a SQL column and break group
 * persistence — these tests fail loudly if that regresses.
 */
class GroupPropertiesTest {

    @Test
    fun `properties getter has no backing field`() {
        val fields = Group::class.java.declaredFields.map { it.name }
        assertTrue("propertiesJson" in fields) { "propertiesJson must be a persisted column" }
        assertFalse("properties" in fields) {
            "properties must be a computed getter (no backing field), otherwise it becomes a SQL column"
        }
    }

    @Test
    fun `properties round-trip through the proto mapper`() {
        val data = GroupData.newBuilder()
            .setName("lobby")
            .setMinMemory(512)
            .setMaxMemory(512)
            .setPlatform("velocity")
            .setVersion("3.5.0")
            .putProperties("fallback", "true")
            .build()

        val group = GroupProtoMapper.toDomain(data)
        assertEquals("true", group.properties["fallback"])

        val backToProto = GroupProtoMapper.toProto(group)
        assertEquals("true", backToProto.propertiesMap["fallback"])
    }

    @Test
    fun `codec tolerates blank and malformed input`() {
        assertTrue(PropertyCodec.decode("").isEmpty())
        assertTrue(PropertyCodec.decode("not-json").isEmpty())
        assertEquals("{\"a\":\"b\"}", PropertyCodec.encode(mapOf("a" to "b")))
    }
}
