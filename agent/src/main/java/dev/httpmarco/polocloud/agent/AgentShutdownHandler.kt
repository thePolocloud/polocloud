package dev.httpmarco.polocloud.agent

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

    if (Thread.currentThread().name != SHUTDOWN_HOOK) {
        exitProcess(-1)
    }
}