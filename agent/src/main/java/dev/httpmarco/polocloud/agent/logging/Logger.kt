package dev.httpmarco.polocloud.agent.logging

import java.time.LocalTime

class Logger {

    private var debugMode = false

    fun info(message: String) {
        log("INFO", "", message)
    }

    fun warn(message: String) {
        log("WARN", "", message)
    }

    fun error(message: String) {
        log("ERROR", "", message)
    }

    fun debug(message: String) {
        if(!debugMode) return
        log("DEBUG", "", message)
    }

    private fun log(level: String, style: String, message: String) {
        val timestamp = LocalTime.now().withNano(0).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
        println("${("$timestamp")} | $style $message")
    }
}