package de.polocloud.node.services

import de.polocloud.shared.service.ServiceState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Minimal in-memory [Process] stand-in: feeds [stdout] to the log reader and captures
 * everything written to stdin, without spawning a real OS process.
 */
private class FakeProcess(
    stdout: String,
    private val alive: Boolean = true,
) : Process() {
    private val input = ByteArrayInputStream(stdout.toByteArray())
    val stdin = ByteArrayOutputStream()

    override fun getOutputStream(): OutputStream = stdin
    override fun getInputStream(): InputStream = input
    override fun getErrorStream(): InputStream = InputStream.nullInputStream()
    override fun waitFor(): Int = 0
    override fun exitValue(): Int = if (alive) throw IllegalThreadStateException() else 0
    override fun destroy() {}
    override fun isAlive(): Boolean = alive
    override fun pid(): Long = 12345L
}

class LocalServiceTest {

    private fun localService() =
        LocalService(Service(UUID.randomUUID(), 1, "lobby", ServiceState.RUNNING, "127.0.0.1", 30000, ""))

    private fun await(timeoutMs: Long = 2000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(10)
        }
        throw AssertionError("Condition not met within ${timeoutMs}ms")
    }

    @Test
    fun `log capture buffers process output`() {
        val service = localService()
        service.process = FakeProcess("line one\nline two\nline three\n")
        service.startLogCapture()

        await { service.recentLogs().size == 3 }
        assertEquals(listOf("line one", "line two", "line three"), service.recentLogs())
    }

    @Test
    fun `log listeners receive live lines and can be removed`() {
        val service = localService()
        service.process = FakeProcess("a\nb\n")

        val received = CopyOnWriteArrayList<String>()
        val latch = CountDownLatch(2)
        val listener: (String) -> Unit = { received += it; latch.countDown() }
        service.addLogListener(listener)

        service.startLogCapture()
        assertTrue(latch.await(2, TimeUnit.SECONDS))
        assertEquals(listOf("a", "b"), received.toList())

        service.removeLogListener(listener)
        // Nothing else should be delivered after removal (buffer still holds the history).
        assertEquals(2, received.size)
    }

    @Test
    fun `the log buffer is capped and drops the oldest lines`() {
        val service = localService()
        val lines = (1..400).joinToString("\n") { "line-$it" } + "\n"
        service.process = FakeProcess(lines)
        service.startLogCapture()

        await { service.recentLogs().size >= 300 }
        val logs = service.recentLogs()
        assertEquals(300, logs.size)
        assertEquals("line-400", logs.last())
        // The oldest 100 lines were evicted.
        assertEquals("line-101", logs.first())
    }

    @Test
    fun `executeCommand writes the command and a newline to stdin`() {
        val service = localService()
        val process = FakeProcess("", alive = true)
        service.process = process

        assertTrue(service.executeCommand("say hello"))
        assertEquals("say hello" + System.lineSeparator(), process.stdin.toString())
    }

    @Test
    fun `executeCommand returns false when there is no process`() {
        assertFalse(localService().executeCommand("noop"))
    }

    @Test
    fun `executeCommand returns false when the process is not alive`() {
        val service = localService()
        service.process = FakeProcess("", alive = false)
        assertFalse(service.executeCommand("noop"))
    }

    @Test
    fun `shutdown wipes an ephemeral work directory`() {
        val dir = java.nio.file.Files.createTempDirectory("polocloud-svc")
        java.nio.file.Files.writeString(dir.resolve("world.dat"), "data")
        val service = localService()
        service.workDir = dir
        service.static = false

        service.shutdown()
        assertFalse(java.nio.file.Files.exists(dir))
    }

    @Test
    fun `shutdown keeps a static service's work directory`() {
        val dir = java.nio.file.Files.createTempDirectory("polocloud-static")
        java.nio.file.Files.writeString(dir.resolve("world.dat"), "data")
        val service = localService()
        service.workDir = dir
        service.static = true

        try {
            service.shutdown()
            assertTrue(java.nio.file.Files.exists(dir))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
