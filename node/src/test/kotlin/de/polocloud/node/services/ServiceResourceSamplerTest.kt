package de.polocloud.node.services

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ServiceResourceSamplerTest {

    @Test
    fun `samples cpu and memory for a live pid`() {
        val sampler = ServiceResourceSampler()
        val usage = sampler.sample(ProcessHandle.current().pid())

        assertNotNull(usage)
        assertTrue(usage!!.cpuUsage >= 0.0)
        assertTrue(usage.usedMemory > 0.0)
    }

    @Test
    fun `returns null for a pid that does not exist`() {
        val sampler = ServiceResourceSampler()
        assertNull(sampler.sample(NON_EXISTENT_PID))
    }

    @Test
    fun `retainOnly drops snapshots for pids no longer alive without breaking future sampling`() {
        val sampler = ServiceResourceSampler()
        val pid = ProcessHandle.current().pid()
        sampler.sample(pid)

        sampler.retainOnly(emptySet())

        assertNotNull(sampler.sample(pid))
    }

    private companion object {
        // Reserved/unlikely-to-exist PID for a "process not found" test case.
        const val NON_EXISTENT_PID = 999_999L
    }
}