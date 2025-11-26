package dev.httpmarco.polocloud.agent.logging

import dev.httpmarco.polocloud.agent.runtime.local.terminal.LoggingColor
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.layout.AbstractStringLayout
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginFactory
import org.jline.jansi.AnsiColors
import java.nio.charset.StandardCharsets
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Plugin(name = "LoggingLayout", category = "Layout", elementType = "layout", printObject = true)
class LoggingLayout : AbstractStringLayout(StandardCharsets.UTF_8) {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    override fun toSerializable(event: LogEvent): String {
        val timestamp = LocalTime.now().withNano(0).format(timeFormatter)

        val color = when (event.level.name()) {
            "INFO" -> "&f"
            "WARN" -> "&e"
            "ERROR" -> "&c"
            "DEBUG" -> "&b"
            else -> "&f"
        }

        return LoggingColor.translate("&7$timestamp &8| $color${event.level}&8: &7${event.message.formattedMessage}\n")
    }

    companion object {
        @JvmStatic
        @PluginFactory
        fun createLayout() = LoggingLayout()
    }
}
