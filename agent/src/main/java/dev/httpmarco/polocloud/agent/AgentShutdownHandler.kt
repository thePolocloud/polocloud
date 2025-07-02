package dev.httpmarco.polocloud.agent

import org.jline.jansi.AnsiConsole
import kotlin.system.exitProcess

private val SHUTDOWN_HOOK = "polocloud-shutdown-hook"
private var idleShutdown = false

fun registerHook() {
    Runtime.getRuntime().addShutdownHook(Thread({
        exitPolocloud()
    }, SHUTDOWN_HOOK))
}

fun exitPolocloud() {

    if (idleShutdown) {
        return
    }

    idleShutdown = true

    Agent.instance.runtime.serviceStorage().items().forEach {
        it.shutdown()
    }

    
    AnsiConsole.systemUninstall()

    logger.info("Polocloud Agent is shutting down&8...")

    if (Thread.currentThread().name != SHUTDOWN_HOOK) {
        exitProcess(-1)
    }
}