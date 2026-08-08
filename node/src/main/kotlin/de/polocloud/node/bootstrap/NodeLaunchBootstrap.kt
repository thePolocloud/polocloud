package de.polocloud.node.bootstrap

import de.polocloud.common.dependency.DependencyRegistry
import de.polocloud.common.dependency.insert.ClasspathInsert
import de.polocloud.common.dependency.scanning.OwnBlobScanner
import de.polocloud.common.system.PolocloudSystemProperties
import java.nio.file.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    // Ensure the console renders UTF-8 output (arrows, emoji, ...) correctly,
    // especially on Windows where the default OEM code page mangles them.
    enableUtf8Console()

    val dependencyRegistry = DependencyRegistry(ClasspathInsert())

    val cliJar = Path.of(System.getProperty(PolocloudSystemProperties.RUNTIME_PATH)).toFile()

    dependencyRegistry.scan(OwnBlobScanner(cliJar))
    dependencyRegistry.downloadAndRegister()

    // try to clean the screen before starting the node
    println("\u001b[H\u001b[2J")

    // fallback exception handler for genuinely unexpected bugs
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        System.err.println("Fatal bootstrap error in thread '${thread.name}'")
        throwable.printStackTrace()
    }

    try {
        val launch = NodeLaunch(args).run()
        launch.start()
    } catch (exception: IllegalStateException) {
        System.err.println("Node startup failed: ${exception.message}")
        generateSequence(exception.cause) { it.cause }.forEach {
            System.err.println("Caused by: ${it.message ?: it.javaClass.name}")
        }
        exitProcess(1)
    }

    // Block main thread until shutdown
    Thread.currentThread().join()
}