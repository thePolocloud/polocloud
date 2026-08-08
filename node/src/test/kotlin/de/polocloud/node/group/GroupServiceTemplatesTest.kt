package de.polocloud.node.group

import de.polocloud.node.group.template.GroupTemplateService
import de.polocloud.node.services.factory.PlatformService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/**
 * Covers [GroupService.applyDefaultTemplates] and the pure [GroupService.roleTemplateFor]
 * helper directly — deliberately not [GroupService.create] itself, which also persists to
 * the database; the defaulting logic is split out precisely so it's testable without one.
 */
class GroupServiceTemplatesTest {

    private val service = GroupService(PlatformService())

    @AfterEach
    fun cleanup() {
        // applyDefaultTemplates ensures the group's own template folder on disk.
        GroupTemplateService.directoryOf("lobby").deleteRecursively()
        GroupTemplateService.directoryOf("proxy").deleteRecursively()
    }

    @Test
    fun `roleTemplateFor picks GLOBAL_PROXY for a proxy platform type`() {
        assertEquals(GroupTemplateService.GLOBAL_PROXY, GroupService.roleTemplateFor("PROXY"))
        assertEquals(GroupTemplateService.GLOBAL_PROXY, GroupService.roleTemplateFor("proxy"))
    }

    @Test
    fun `roleTemplateFor falls back to GLOBAL_SERVER for anything else, including unknown`() {
        assertEquals(GroupTemplateService.GLOBAL_SERVER, GroupService.roleTemplateFor("SERVER"))
        assertEquals(GroupTemplateService.GLOBAL_SERVER, GroupService.roleTemplateFor(null))
    }

    @Test
    fun `a group with no templates gets GLOBAL, its role template and its own name`() {
        // PlatformService() has no platforms loaded, so the platform type is unresolved
        // and roleTemplateFor falls back to GLOBAL_SERVER — this exercises that same
        // fallback end to end through applyDefaultTemplates.
        val group = Group("lobby", 512, 0.5, 1, 3, "paper", "1.21")

        val result = service.applyDefaultTemplates(group)

        assertEquals(listOf(GroupTemplateService.GLOBAL, GroupTemplateService.GLOBAL_SERVER, "lobby"), result.templates)
    }

    @Test
    fun `a group that already has templates is left untouched`() {
        val group = Group("proxy", 512, 0.5, 1, 3, "velocity", "3.5.0")
            .copy(templatesJson = TemplateCodec.encode(listOf("custom")))

        val result = service.applyDefaultTemplates(group)

        assertSame(group, result)
        assertEquals(listOf("custom"), result.templates)
    }
}
