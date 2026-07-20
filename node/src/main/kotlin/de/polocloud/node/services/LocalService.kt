package de.polocloud.node.services

import de.polocloud.shared.service.ServiceState
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

class LocalService(private val service: Service) : Service(
    service.id, service.index, service.groupName, service.state, service.hostname, service.port, service.nodeId
) {

    private companion object {
        val logger = LoggerFactory.getLogger(LocalService::class.java)

        /** How many recent log lines are retained per service for the `logs` tail. */
        const val LOG_BUFFER_CAPACITY = 300
    }

    var process: Process? = null
    var workDir: Path? = null

    /**
     * Millis timestamp of when [process] was actually started. Used by
     * [de.polocloud.node.services.queue.CrashLoopGuard] to tell a service that crashed
     * moments after launch from one that ran fine for a while and then legitimately
     * stopped — not a persisted [Service] column, only meaningful for the lifetime of this
     * in-memory instance.
     */
    var startedAt: Long = 0

    /**
     * Most recently sampled snapshot of this process's descendant OS processes, refreshed
     * by [de.polocloud.node.services.ping.ServicePingFactory] while [process] is alive.
     *
     * A process's descendants can only be enumerated while it is still running — once it
     * exits (which is exactly when a crash is detected; see [de.polocloud.node.services.factory.FactoryService]'s
     * `onExit` hook), the OS no longer reports what its children were, so
     * `process.toHandle().descendants()` comes back empty from that point on. Keeping this
     * rolling snapshot lets [shutdown] still reap children the process spawned before it
     * died, instead of only ever seeing an empty tree for a root that was already dead by
     * the time cleanup ran.
     */
    @Volatile
    var lastKnownDescendants: List<ProcessHandle> = emptyList()

    /** Refreshes [lastKnownDescendants] from the live process tree; a no-op once [process] has exited. */
    fun sampleDescendants() {
        val proc = process ?: return
        if (!proc.isAlive) return
        lastKnownDescendants = proc.toHandle().descendants().toList()
    }

    /**
     * Whether this service belongs to a static group. Static services keep their work
     * directory (world/config) across restarts instead of being wiped on shutdown.
     */
    var static: Boolean = false

    /**
     * Free-form key/value properties, seeded from the owning group when the service
     * starts. Kept in-memory only (not a persisted [Service] column) — services are
     * ephemeral and re-seed their properties from the group on every start.
     */
    val properties: MutableMap<String, String> = mutableMapOf()

    /**
     * Players currently connected / configured player slots, as last reported by
     * [de.polocloud.node.services.ping.ServicePingFactory] over the Minecraft Server List
     * Ping. `0` until the first successful ping. These are only ever written by the ping
     * loop — nothing else in the node should assign them, keeping the value a read-only
     * reflection of what the service itself reports.
     */
    var onlinePlayers: Int = 0
    var maxPlayers: Int = 0

    /**
     * MOTD text from the last successful [de.polocloud.node.services.ping.ServicePingFactory]
     * ping. Empty until the first successful ping. Only ever written by the ping loop.
     */
    var motd: String = ""

    /** Millis timestamp of the last player-count ping; used to throttle [de.polocloud.node.services.ping.ServicePingFactory] polling of already-running services. */
    var lastPlayerPollAt: Long = 0

    /**
     * CPU usage percent (0-100) and resident memory (MB) of this service's OS process, as
     * last sampled by [de.polocloud.node.services.ping.ServicePingFactory] via
     * [ServiceResourceSampler]. Both `0.0` until the first sample. Only ever written by
     * that sampling loop — nothing else in the node should assign these, same as
     * [onlinePlayers]/[motd].
     */
    var cpuUsage: Double = 0.0
    var usedMemory: Double = 0.0

    /**
     * Names of the templates applied to this service's work directory on start, in the
     * order they were copied (see [de.polocloud.node.group.template.GroupTemplateService]).
     * Snapshotted from the owning group at start time — not re-read from the group
     * afterwards, so it reflects what this specific instance actually got, even if the
     * group's template list changes later.
     */
    var templates: List<String> = emptyList()

    // Ring buffer of recent stdout/stderr lines (the process is started with
    // redirectErrorStream=true, so both arrive on the same stream).
    private val logBuffer = ArrayDeque<String>(LOG_BUFFER_CAPACITY)

    // Live consumers (e.g. an open `service <name> logs` stream). CopyOnWrite so the
    // reader thread can iterate while a CLI stream subscribes/unsubscribes concurrently.
    private val logListeners = CopyOnWriteArrayList<(String) -> Unit>()

    private var logReader: Thread? = null

    // Guards [shutdown] against running twice: the process's own exit (crash, or `/stop`
    // typed in its console) and an operator-issued shutdown can race each other, and the
    // process-tree kill / workDir deletion below must complete exactly once — running it
    // twice concurrently could delete files the other invocation is still using.
    private val cleanedUp = AtomicBoolean(false)

    /**
     * Starts a daemon thread that pumps the process output into [logBuffer] and to
     * every registered [logListeners] entry. Call once, right after the process starts.
     */
    fun startLogCapture() {
        val proc = process ?: return
        val reader = Thread({
            runCatching {
                proc.inputStream.bufferedReader().use { buffered: BufferedReader ->
                    buffered.forEachLine { line ->
                        appendLog(line)
                    }
                }
            }
        }, "service-log-${name()}").apply { isDaemon = true }
        logReader = reader
        reader.start()
    }

    private fun appendLog(line: String) {
        synchronized(logBuffer) {
            if (logBuffer.size >= LOG_BUFFER_CAPACITY) logBuffer.removeFirst()
            logBuffer.addLast(line)
        }
        // Isolate listeners: a slow/failing consumer must not stall log capture.
        logListeners.forEach { listener -> runCatching { listener(line) } }
    }

    /** A snapshot of the currently buffered recent log lines. */
    fun recentLogs(): List<String> = synchronized(logBuffer) { logBuffer.toList() }

    fun addLogListener(listener: (String) -> Unit) {
        logListeners += listener
    }

    fun removeLogListener(listener: (String) -> Unit) {
        logListeners -= listener
    }

    /**
     * Writes [command] to the process's stdin (followed by a newline) so the service
     * executes it in its own console. Returns `false` if the process is not running.
     */
    fun executeCommand(command: String): Boolean {
        val proc = process ?: return false
        if (!proc.isAlive) return false
        return runCatching {
            val stdin = proc.outputStream
            stdin.write((command + System.lineSeparator()).toByteArray())
            stdin.flush()
            true
        }.getOrElse {
            logger.warn("Failed to write command to {}: {}", name(), it.message)
            false
        }
    }

    /**
     * Terminates the process (if any), persists the resulting state and cleans up the
     * work directory. Returns `false` without doing anything if a concurrent caller is
     * already running (or has finished) this same cleanup — see [cleanedUp].
     */
    @OptIn(ExperimentalPathApi::class)
    fun shutdown(): Boolean {
        if (!cleanedUp.compareAndSet(false, true)) return false
        logListeners.clear()
        process?.let { process ->
            val handle = process.toHandle()
            // If the root already exited on its own (a crash, or `/stop` in the service's
            // console — this same method also runs from FactoryService's onExit hook,
            // after the fact), its descendants can no longer be enumerated at all; only
            // lastKnownDescendants (sampled while it was still alive) still knows what to
            // kill. Merge both so a still-live commanded shutdown gets the fully current
            // tree, while a reactive crash cleanup gets whatever was last seen. Windows
            // does not cascade termination — leftover children would be orphaned otherwise.
            val tree = (lastKnownDescendants + handle.descendants().toList() + handle).distinct()

            // Ask the whole tree to terminate gracefully, then give it a moment.
            tree.forEach { runCatching { it.destroy() } }
            runCatching { process.waitFor(5, TimeUnit.SECONDS) }

            // Force-kill anything that ignored the graceful request.
            tree.filter { it.isAlive }.forEach { runCatching { it.destroyForcibly() } }
            runCatching { process.waitFor(2, TimeUnit.SECONDS) }

            service.state = ServiceState.STOPPED
        }

        // Best-effort: a failing repository delete must not skip the work-directory cleanup below.
        runCatching { ServiceRepository.delete(service) }.onFailure {
            logger.warn("Failed to delete service {} (id={}) from the database: {}", service.name(), service.id, it.message)
        }
        // The database library silently discards the affected-row count from its DELETE —
        // a WHERE clause matching zero rows produces no exception or log anywhere in it.
        // Reading the row back is the only way to notice a delete that quietly did nothing
        // (e.g. a stale schema after a column was added) instead of it only ever surfacing
        // later as an unexplained stale row.
        if (runCatching { ServiceRepository.findById(service.id) != null }.getOrDefault(false)) {
            logger.warn("Service {} (id={}) still exists in the database after delete — the row may now be stale", service.name(), service.id)
        }

        // Give the OS a moment to release file handles before deleting the work
        // directory (Windows keeps the jar locked briefly after the process exits).
        if (!Thread.currentThread().isVirtual) {
            Thread.sleep(200)
        }

        // Static services keep their work directory (persistent world/config); only
        // ephemeral services get their directory wiped on shutdown. Best-effort: a locked
        // file (Windows) must not fail the whole shutdown — cleanup elsewhere (DB row,
        // localServices removal) is already done and must stand regardless.
        if (!static) {
            runCatching { workDir?.deleteRecursively() }.onFailure {
                logger.warn("Failed to delete work directory {} for {}: {}", workDir, service.name(), it.message)
            }
        }
        return true
    }

}
