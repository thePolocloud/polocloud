package de.polocloud.node.module

import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import kotlin.concurrent.thread

/**
 * Watches [folder] (non-recursively — modules are single flat jars, no subfolders) for
 * jars being added, replaced or removed, so dropping a new module jar in while the node
 * is running loads it without needing an explicit `reload`.
 */
class ModuleFolderWatcher(
    folder: File,
    private val onJarChanged: (File) -> Unit,
    private val onJarRemoved: (File) -> Unit,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val watchService = FileSystems.getDefault().newWatchService()

    @Volatile
    private var running = true

    init {
        folder.toPath().register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE,
        )
        thread(isDaemon = true, name = "polocloud-module-watcher[${folder.name}]") {
            while (running) {
                val key = runCatching { watchService.take() }.getOrNull() ?: break

                key.pollEvents().forEach { event ->
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) return@forEach

                    @Suppress("UNCHECKED_CAST")
                    val name = (event as WatchEvent<Path>).context().toString()
                    if (!name.endsWith(".jar")) return@forEach

                    val file = File(folder, name)

                    runCatching {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                            onJarRemoved(file)
                        } else if (awaitStableFile(file)) {
                            onJarChanged(file)
                        }
                    }.onFailure { logger.error("Module watcher failed to handle '{}': {}", name, it.message, it) }
                }

                if (!key.reset()) break
            }
        }
    }

    fun stop() {
        running = false
        runCatching { watchService.close() }
    }

    /**
     * Polls [file]'s size until two consecutive reads, [STABILIZE_POLL_MS] apart, agree —
     * a jar being copied/moved in fires several MODIFY events before the write finishes,
     * so a fixed "wait a beat" debounce either fires too early on a slow copy (large jar,
     * slow disk/network mount) or wastes time on a fast one. Gives up and proceeds anyway
     * after [STABILIZE_MAX_WAIT_MS] so a copy that never truly settles can't wedge the
     * watcher thread forever. Returns `false` if the file disappeared while waiting (e.g.
     * a rename-then-delete raced us).
     */
    private fun awaitStableFile(file: File): Boolean {
        var previousSize = -1L
        val deadline = System.currentTimeMillis() + STABILIZE_MAX_WAIT_MS
        while (System.currentTimeMillis() < deadline) {
            if (!file.isFile) return false
            val size = file.length()
            if (size == previousSize) return true
            previousSize = size
            Thread.sleep(STABILIZE_POLL_MS)
        }
        return file.isFile
    }

    private companion object {
        const val STABILIZE_POLL_MS = 100L
        const val STABILIZE_MAX_WAIT_MS = 10_000L
    }
}
