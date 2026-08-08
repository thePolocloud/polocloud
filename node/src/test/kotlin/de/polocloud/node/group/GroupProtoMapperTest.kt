package de.polocloud.node.group

import de.polocloud.proto.GroupData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GroupProtoMapperTest {

    @Test
    fun `toProto exposes the decoded properties`() {
        val group = Group(
            name = "Lobby", memory = 512, startThreshold = 0.5, minOnline = 1, maxOnline = 3,
            platform = "velocity", version = "3.5.0",
        ).copy(propertiesJson = """{"fallback":"true"}""")
        val data = GroupProtoMapper.toProto(group)
        assertEquals("Lobby", data.name)
        assertEquals("true", data.propertiesMap["fallback"])
    }

    @Test
    fun `toDomain stores properties as json`() {
        val data = GroupData.newBuilder()
            .setName("Lobby").setMemory(512).setPlatform("velocity").setVersion("3.5.0")
            .putProperties("fallback", "true")
            .build()
        val group = GroupProtoMapper.toDomain(data)
        assertEquals("true", group.properties["fallback"])
        // Persisted representation is JSON, not a map column.
        assertTrue(group.propertiesJson.contains("fallback"))
    }

    @Test
    fun `round-trips properties`() {
        val data = GroupData.newBuilder()
            .setName("Lobby").setMemory(1024).setStartThreshold(0.7).setMinOnline(1).setMaxOnline(3)
            .setPlatform("paper").setVersion("1.21")
            .putProperties("region", "eu").putProperties("fallback", "true")
            .build()

        val restored = GroupProtoMapper.toProto(GroupProtoMapper.toDomain(data))
        assertEquals(data.propertiesMap, restored.propertiesMap)
        assertEquals("paper", restored.platform)
        assertEquals(1024, restored.memory)
    }

    @Test
    fun `group without properties yields an empty proto map`() {
        val group = Group("Lobby", 512, 0.0, 0, 1, "velocity", "3.5.0")
        assertTrue(GroupProtoMapper.toProto(group).propertiesMap.isEmpty())
    }
}
