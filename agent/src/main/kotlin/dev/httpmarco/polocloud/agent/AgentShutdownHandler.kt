package dev.httpmarco.polocloud.agent

import org.jline.jansi.AnsiConsole
import kotlin.io.path.Path
import kotlin.system.exitProcess

private val SHUTDOWN_HOOK = "polocloud-shutdown-hook"
private var inShutdown = false

fun registerHook() {
    Runtime.getRuntime().addShutdownHook(Thread({
        exitPolocloud()
    }, SHUTDOWN_HOOK))
}

fun exitPolocloud(cleanShutdown: Boolean = true) {

    if (inShutdown) {
        return
    }

    inShutdown = true

    i18n.info("agent.shutdown.starting")


    try {
        Agent.runtime.serviceStorage().items().forEach {
            it.shutdown(cleanShutdown)
        }

        Agent.close()

        i18n.info("agent.shutdown.temp-files.cleanup")
        Path("temp").toFile().deleteRecursively()
        i18n.info("agent.shutdown.temp-files.cleanup.successful")

        AnsiConsole.systemUninstall()
    } catch (e: Exception) {
        logger.throwable(e)
    }

    i18n.info("agent.shutdown.successful")

    if (Thread.currentThread().name != SHUTDOWN_HOOK) {
        exitProcess(0)
    }
}

fun shutdownProcess(): Boolean {
    return inShutdown
}