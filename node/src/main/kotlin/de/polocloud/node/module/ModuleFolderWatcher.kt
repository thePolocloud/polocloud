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
                    // Debounce: a jar being copied in fires several MODIFY events before
                    // the copy finishes; wait a beat so we read a complete file.
                    Thread.sleep(300)

                    runCatching {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                            onJarRemoved(file)
                        } else if (file.isFile) {
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
}
