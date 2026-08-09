package de.polocloud.node.module

import de.polocloud.common.version.PolocloudVersion
import de.polocloud.moduleapi.ModuleDescriptor
import de.polocloud.moduleapi.ModuleScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Covers [ModuleManager]'s pure decision logic (load ordering, the SINGLE_ACTIVE
 * enable/disable policy, api-version compatibility) directly via its `internal` companion
 * functions — without needing a real jar, classloader or [de.polocloud.node.cluster.election.NodeElectionService],
 * which the full load/enable pipeline (exercised manually via a real module jar) requires.
 */
class ModuleManagerPolicyTest {

    private fun descriptor(
        name: String,
        depends: List<String> = emptyList(),
        softDepends: List<String> = emptyList(),
    ) = ModuleDescriptor(name = name, version = "1.0.0", main = "irrelevant.Main", depends = depends, softDepends = softDepends)

    @Test
    fun `resolveLoadOrder places hard dependencies before their dependents`() {
        val a = descriptor("a")
        val b = descriptor("b", depends = listOf("a"))
        val c = descriptor("c", depends = listOf("b"))

        // Deliberately out of order on input — the sort must fix that up.
        val order = ModuleManager.resolveLoadOrder(listOf(c, a, b)).map { it.name }

        assertEquals(listOf("a", "b", "c"), order)
    }

    @Test
    fun `resolveLoadOrder respects soft-depends ordering when present`() {
        val a = descriptor("a")
        val b = descriptor("b", softDepends = listOf("a"))

        val order = ModuleManager.resolveLoadOrder(listOf(b, a)).map { it.name }

        assertEquals(listOf("a", "b"), order)
    }

    @Test
    fun `resolveLoadOrder ignores a missing soft-depend instead of failing`() {
        val onlyModule = descriptor("only", softDepends = listOf("nonexistent"))

        val order = ModuleManager.resolveLoadOrder(listOf(onlyModule)).map { it.name }

        assertEquals(listOf("only"), order)
    }

    @Test
    fun `resolveLoadOrder breaks a circular hard dependency instead of hanging`() {
        val a = descriptor("a", depends = listOf("b"))
        val b = descriptor("b", depends = listOf("a"))

        val order = ModuleManager.resolveLoadOrder(listOf(a, b)).map { it.name }.toSet()

        // Both are still returned exactly once each — the cycle is broken, not dropped.
        assertEquals(setOf("a", "b"), order)
    }

    @Test
    fun `EVERY_NODE modules are always enabled regardless of head status`() {
        assertTrue(ModuleManager.shouldBeEnabled(ModuleScope.EVERY_NODE, isHead = true))
        assertTrue(ModuleManager.shouldBeEnabled(ModuleScope.EVERY_NODE, isHead = false))
    }

    @Test
    fun `SINGLE_ACTIVE modules are only enabled when this node is head`() {
        assertTrue(ModuleManager.shouldBeEnabled(ModuleScope.SINGLE_ACTIVE, isHead = true))
        assertFalse(ModuleManager.shouldBeEnabled(ModuleScope.SINGLE_ACTIVE, isHead = false))
    }

    @Test
    fun `api-version requires a matching major version`() {
        val declared = PolocloudVersion(major = 3, minor = 0, patch = 0)
        val sameMajorNewer = PolocloudVersion(major = 3, minor = 2, patch = 0)
        val differentMajor = PolocloudVersion(major = 4, minor = 0, patch = 0)

        assertTrue(ModuleManager.isApiCompatible(current = sameMajorNewer, declared = declared))
        assertFalse(ModuleManager.isApiCompatible(current = differentMajor, declared = declared))
    }

    @Test
    fun `api-version rejects a node older than the declared minor-patch`() {
        val declared = PolocloudVersion(major = 3, minor = 2, patch = 0)
        val olderMinor = PolocloudVersion(major = 3, minor = 1, patch = 9)

        assertFalse(ModuleManager.isApiCompatible(current = olderMinor, declared = declared))
        assertTrue(ModuleManager.isApiCompatible(current = declared, declared = declared))
    }
}
