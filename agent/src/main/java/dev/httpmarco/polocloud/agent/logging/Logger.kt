package dev.httpmarco.polocloud.agent.logging

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.local.LocalRuntime
import dev.httpmarco.polocloud.agent.runtime.local.terminal.LoggingColor
import java.time.LocalTime

class Logger {

    private var debugMode = false

    fun info(message: String) {
        log("INFO", "&f", message)
    }

    fun warn(message: String) {
        log("WARN", "&e", message)
    }

    fun error(message: String) {
        log("ERROR", "&c", message)
    }

    fun throwable(throwable: Throwable) {
        // Handle other exceptions that may occur during reading
        logger.error("An error occurred thread: ${throwable.message}")

        // for a better debugging experience, we print the stack trace
        throwable.stackTrace.forEach {
            logger.error("  at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})")
        }
    }

    fun debug(message: String) {
        if (!debugMode) return
        log("DEBUG", "&f", message)
    }

    private fun log(level: String, style: String, message: String) {
        val timestamp = LocalTime.now().withNano(0).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
        val message = LoggingColor.translate("${("$timestamp")} &8| $style$level&8: &7$message")

        // if the agent is running in a local runtime, we use the terminal to display the message
        val agent = Agent.instance

        if(agent != null && agent.runtime != null && agent.runtime is LocalRuntime) {
            val localRuntime = Agent.instance.runtime

            localRuntime.terminal.display(message)
            return
        }

        println(message)
    }
}