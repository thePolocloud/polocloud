package dev.httpmarco.polocloud.agent.logging

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

    fun debug(message: String) {
        if(!debugMode) return
        log("DEBUG", "&f", message)
    }

    private fun log(level: String, style: String, message: String) {
        val timestamp = LocalTime.now().withNano(0).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
        println(LoggingColor.translate("${("$timestamp")} &8| $style$level&8: &7$message"))
    }
}