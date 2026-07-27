package de.polocloud.api.group

import de.polocloud.shared.property.Properties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GroupBuilderTest {

    private var submitted: Group? = null
    private fun builder() = GroupBuilder { group -> submitted = group; group }

    @Test
    fun `submit forwards the configured group`() {
        val result = builder()
            .name("Lobby")
            .memory(1024)
            .startThreshold(0.7)
            .minOnline(1)
            .maxOnline(3)
            .platform("velocity")
            .version("3.5.0")
            .submit()

        assertEquals("Lobby", result.name)
        assertEquals(1024, result.memory)
        assertEquals(0.7, result.startThreshold)
        assertEquals(1, result.minOnline)
        assertEquals(3, result.maxOnline)
        assertEquals("velocity", result.platform)
        assertEquals("3.5.0", result.version)
        assertEquals(result, submitted)
    }

    @Test
    fun `defaults are applied when not overridden`() {
        val group = builder().name("Lobby").submit()
        assertEquals(512, group.memory)
        assertEquals(0.0, group.startThreshold)
        assertEquals(0, group.minOnline)
        assertEquals(1, group.maxOnline)
        assertTrue(group.properties.isEmpty())
    }

    @Test
    fun `a blank name is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            builder().memory(256).submit()
        }
    }

    @Test
    fun `property sets a single entry`() {
        val group = builder().name("Lobby").property("region", "eu").submit()
        assertEquals("eu", group.properties["region"])
    }

    @Test
    fun `properties merges a map`() {
        val group = builder().name("Lobby").properties(mapOf("a" to "1", "b" to "2")).submit()
        assertEquals("1", group.properties["a"])
        assertEquals("2", group.properties["b"])
    }

    @Test
    fun `template appends to the ordered template list`() {
        val group = builder().name("Lobby").template("GLOBAL").template("Lobby").submit()
        assertEquals(listOf("GLOBAL", "Lobby"), group.templates)
    }

    @Test
    fun `templates appends multiple entries in order`() {
        val group = builder().name("Lobby").templates("GLOBAL", "GLOBAL_SERVER").template("Lobby").submit()
        assertEquals(listOf("GLOBAL", "GLOBAL_SERVER", "Lobby"), group.templates)
    }

    @Test
    fun `fallback toggles the fallback property`() {
        val on = builder().name("Lobby").fallback().submit()
        assertTrue(on.isFallback())
        assertEquals("true", on.properties[Properties.FALLBACK])

        val off = builder().name("Lobby").fallback(false).submit()
        assertFalse(off.isFallback())
    }
}
