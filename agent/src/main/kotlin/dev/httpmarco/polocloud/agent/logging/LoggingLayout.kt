package dev.httpmarco.polocloud.agent.logging

import dev.httpmarco.polocloud.agent.runtime.local.terminal.LoggingColor
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.layout.AbstractStringLayout
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginFactory
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

        val msg = StringBuilder()
        msg.append(LoggingColor.translate("&7$timestamp &8| $color${event.level}&8: &7${event.message.formattedMessage}\n"))

        event.thrown?.let { throwable ->
            throwable.stackTrace.forEach { element ->
                msg.append(LoggingColor.translate("&7\tat ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})\n"))
            }
            throwable.cause?.let { cause ->
                msg.append(LoggingColor.translate("&7Caused by: ${cause}\n"))
            }
        }

        return msg.toString()
    }

    companion object {
        @JvmStatic
        @PluginFactory
        fun createLayout() = LoggingLayout()
    }
}
