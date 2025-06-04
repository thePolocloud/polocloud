package dev.httpmarco.polocloud.agent.logging

import com.github.ajalt.mordant.rendering.TextColors.Companion.rgb
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.rendering.TextStyle
import java.time.LocalTime

class Logger {

    private val terminal = Terminal()
    private var debugMode = false

    fun info(message: String) {
        log("INFO", rgb("#ffffff"), message)
    }

    fun warn(message: String) {
        log("WARN", rgb("#FF9900"), message)
    }

    fun error(message: String) {
        log("ERROR", rgb("#FF4136"), message)
    }

    fun debug(message: String) {
        if(!debugMode) return
        log("DEBUG", rgb("#969696"), message)
    }

    private fun log(level: String, style: TextStyle, message: String) {
        val timestamp = LocalTime.now().withNano(0).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
        terminal.println("${("$timestamp")} | ${style("$level:")} $message")
    }
}