package de.polocloud.node.group

import de.polocloud.proto.GroupData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Guards the persistence-safe design of group templates, mirroring [GroupPropertiesTest]:
 * [Group.templates] must stay a computed getter over [Group.templatesJson], never a
 * backing field, or it would silently become its own (unsupported) SQL column.
 */
class GroupTemplatesTest {

    @Test
    fun `templates getter has no backing field`() {
        val fields = Group::class.java.declaredFields.map { it.name }
        assertTrue("templatesJson" in fields) { "templatesJson must be a persisted column" }
        assertFalse("templates" in fields) {
            "templates must be a computed getter (no backing field), otherwise it becomes a SQL column"
        }
    }

    @Test
    fun `templates default to empty`() {
        val group = Group("lobby", 512, 0.5, 1, 3, "velocity", "3.5.0")
        assertTrue(group.templates.isEmpty())
    }

    @Test
    fun `templates preserve insertion order`() {
        val group = Group("lobby", 512, 0.5, 1, 3, "velocity", "3.5.0")
            .copy(templatesJson = TemplateCodec.encode(listOf("GLOBAL", "GLOBAL_PROXY", "lobby")))
        assertEquals(listOf("GLOBAL", "GLOBAL_PROXY", "lobby"), group.templates)
    }

    @Test
    fun `templates round-trip through the proto mapper`() {
        val data = GroupData.newBuilder()
            .setName("lobby")
            .setMemory(512)
            .setPlatform("velocity")
            .setVersion("3.5.0")
            .addAllTemplates(listOf("GLOBAL", "GLOBAL_PROXY", "lobby"))
            .build()

        val group = GroupProtoMapper.toDomain(data)
        assertEquals(listOf("GLOBAL", "GLOBAL_PROXY", "lobby"), group.templates)

        val backToProto = GroupProtoMapper.toProto(group)
        assertEquals(listOf("GLOBAL", "GLOBAL_PROXY", "lobby"), backToProto.templatesList)
    }
}
