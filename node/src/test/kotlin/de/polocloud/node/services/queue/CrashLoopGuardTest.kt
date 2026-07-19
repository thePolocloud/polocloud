package de.polocloud.node.services.queue

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Covers [CrashLoopGuard]'s fast-failure counting and backoff decision. Pure in-memory
 * state keyed by group name, so no database or real waiting is needed: a triggered
 * backoff window is always at least 5s long (`BASE_BACKOFF_MILLIS`), which comfortably
 * outlasts the time it takes this test to assert [CrashLoopGuard.isBackingOff] right
 * after triggering it.
 */
class CrashLoopGuardTest {

    @Test
    fun `a group that has never crashed is not backing off`() {
        val guard = CrashLoopGuard()

        assertFalse(guard.isBackingOff("lobby"))
    }

    @Test
    fun `fast failures below the threshold do not trigger backoff`() {
        val guard = CrashLoopGuard()

        guard.recordExit("lobby", ranForMillis = 100)
        guard.recordExit("lobby", ranForMillis = 100)

        assertFalse(guard.isBackingOff("lobby"))
    }

    @Test
    fun `the third consecutive fast failure in a row triggers backoff`() {
        val guard = CrashLoopGuard()

        guard.recordExit("lobby", ranForMillis = 100)
        guard.recordExit("lobby", ranForMillis = 100)
        guard.recordExit("lobby", ranForMillis = 100)

        assertTrue(guard.isBackingOff("lobby"))
    }

    @Test
    fun `a run that survives past the fast-failure window resets the streak`() {
        val guard = CrashLoopGuard()

        guard.recordExit("lobby", ranForMillis = 100)
        guard.recordExit("lobby", ranForMillis = 100)
        // Survives long enough to not count as "fast" - resets the streak back to 0.
        guard.recordExit("lobby", ranForMillis = 60_000)
        guard.recordExit("lobby", ranForMillis = 100)
        guard.recordExit("lobby", ranForMillis = 100)

        assertFalse(guard.isBackingOff("lobby"))
    }

    @Test
    fun `backoff is scoped per group, not shared globally`() {
        val guard = CrashLoopGuard()

        guard.recordExit("lobby", ranForMillis = 100)
        guard.recordExit("lobby", ranForMillis = 100)
        guard.recordExit("lobby", ranForMillis = 100)

        assertTrue(guard.isBackingOff("lobby"))
        assertFalse(guard.isBackingOff("survival"))
    }
}