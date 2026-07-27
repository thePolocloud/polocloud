package de.polocloud.api.group

import de.polocloud.proto.GroupData
import de.polocloud.shared.property.Properties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GroupMapperTest {

    @Test
    fun `toApi maps every field including properties`() {
        val data = GroupData.newBuilder()
            .setName("Lobby")
            .setMemory(1024)
            .setStartThreshold(0.7)
            .setMinOnline(1)
            .setMaxOnline(3)
            .setPlatform("velocity")
            .setVersion("3.5.0")
            .putProperties(Properties.FALLBACK, "true")
            .putProperties("region", "eu")
            .addTemplates("GLOBAL")
            .addTemplates("Lobby")
            .build()

        val group = GroupMapper.toApi(data)

        assertEquals("Lobby", group.name)
        assertEquals(1024, group.memory)
        assertEquals(0.7, group.startThreshold)
        assertEquals(1, group.minOnline)
        assertEquals(3, group.maxOnline)
        assertEquals("velocity", group.platform)
        assertEquals("3.5.0", group.version)
        assertTrue(group.isFallback())
        assertEquals("eu", group.properties["region"])
        assertEquals(listOf("GLOBAL", "Lobby"), group.templates)
    }

    @Test
    fun `toProto maps every field including properties`() {
        val group = Group(
            name = "Lobby", memory = 512, startThreshold = 0.5, minOnline = 0, maxOnline = 2,
            platform = "paper", version = "1.21",
            properties = Properties().set("region", "us"),
            templates = listOf("GLOBAL", "GLOBAL_SERVER"),
        )

        val data = GroupMapper.toProto(group)

        assertEquals("Lobby", data.name)
        assertEquals(512, data.memory)
        assertEquals("paper", data.platform)
        assertEquals("us", data.propertiesMap["region"])
        assertEquals(listOf("GLOBAL", "GLOBAL_SERVER"), data.templatesList)
    }

    @Test
    fun `round-trips through proto without loss`() {
        val group = Group(
            name = "Lobby", memory = 1024, startThreshold = 0.7, minOnline = 1, maxOnline = 3,
            platform = "velocity", version = "3.5.0",
            properties = Properties().set(Properties.FALLBACK, "true"),
            templates = listOf("GLOBAL", "Lobby"),
            nodes = listOf("node-1"),
        )
        assertEquals(group, GroupMapper.toApi(GroupMapper.toProto(group)))
    }

    @Test
    fun `empty properties round-trip to an empty map`() {
        val group = Group("Lobby", 512, 0.0, 0, 1, "velocity", "3.5.0")
        val data = GroupMapper.toProto(group)
        assertTrue(data.propertiesMap.isEmpty())
        assertFalse(GroupMapper.toApi(data).isFallback())
    }
}
