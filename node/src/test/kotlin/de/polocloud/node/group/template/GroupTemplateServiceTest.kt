package de.polocloud.node.group.template

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID
import kotlin.io.path.createTempDirectory

class GroupTemplateServiceTest {

    // Real (but disposable) folders under the actual local/templates/ root, since the
    // service resolves that path itself rather than taking it as a parameter — same
    // trade-off as PlatformService's hardcoded .cache/platforms. Every folder created
    // here is prefixed uniquely and removed again in [cleanup].
    private val createdTemplates = mutableListOf<String>()
    private val targets = mutableListOf<File>()

    private fun uniqueTemplate(): String = "test-${UUID.randomUUID()}".also { createdTemplates += it }

    @AfterEach
    fun cleanup() {
        createdTemplates.forEach { GroupTemplateService.directoryOf(it).deleteRecursively() }
        targets.forEach { it.deleteRecursively() }
    }

    @Test
    fun `ensure creates the template folder`() {
        val name = uniqueTemplate()
        val dir = GroupTemplateService.directoryOf(name)
        assertFalse(dir.exists())

        GroupTemplateService.ensure(name)

        assertTrue(dir.isDirectory)
    }

    @Test
    fun `ensure is idempotent`() {
        val name = uniqueTemplate()
        GroupTemplateService.ensure(name)
        GroupTemplateService.ensure(name)

        assertTrue(GroupTemplateService.directoryOf(name).isDirectory)
    }

    @Test
    fun `copyInto lays a template's files into the target directory`() {
        val name = uniqueTemplate()
        val dir = GroupTemplateService.directoryOf(name)
        dir.mkdirs()
        File(dir, "server.properties").writeText("motd=hello")

        val target = createTempDirectory().toFile().also { targets += it }
        GroupTemplateService.copyInto(listOf(name), target)

        assertEquals("motd=hello", File(target, "server.properties").readText())
    }

    @Test
    fun `later templates overwrite earlier ones on conflict`() {
        val first = uniqueTemplate()
        val second = uniqueTemplate()
        File(GroupTemplateService.directoryOf(first).apply { mkdirs() }, "motd.txt").writeText("first")
        File(GroupTemplateService.directoryOf(second).apply { mkdirs() }, "motd.txt").writeText("second")

        val target = createTempDirectory().toFile().also { targets += it }
        GroupTemplateService.copyInto(listOf(first, second), target)

        assertEquals("second", File(target, "motd.txt").readText())
    }

    @Test
    fun `a missing template folder is skipped, not an error`() {
        val target = createTempDirectory().toFile().also { targets += it }

        // Must not throw even though this template was never created.
        GroupTemplateService.copyInto(listOf("does-not-exist-${UUID.randomUUID()}"), target)

        assertTrue(target.listFiles()?.isEmpty() != false)
    }

    @Test
    fun `directoryOf rejects a name that would escape the templates root`() {
        assertThrows(IllegalArgumentException::class.java) { GroupTemplateService.directoryOf("../secrets") }
        assertThrows(IllegalArgumentException::class.java) { GroupTemplateService.directoryOf("..") }
        assertThrows(IllegalArgumentException::class.java) { GroupTemplateService.directoryOf("a/../../b") }
    }

    @Test
    fun `copyInto skips a traversal attempt instead of reading outside the templates root`() {
        val target = createTempDirectory().toFile().also { targets += it }

        // Must not throw and must not copy anything from outside local/templates/.
        GroupTemplateService.copyInto(listOf("../../.."), target)

        assertTrue(target.listFiles()?.isEmpty() != false)
    }
}
