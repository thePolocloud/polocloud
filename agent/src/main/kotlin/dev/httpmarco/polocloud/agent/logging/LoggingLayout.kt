package dev.httpmarco.polocloud.agent.logging

import dev.httpmarco.polocloud.agent.runtime.local.terminal.LoggingColor
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginFactory
import org.apache.logging.log4j.core.layout.AbstractStringLayout
import java.nio.charset.StandardCharsets
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Plugin(
    name = "LoggingLayout",
    category = "Layout",
    elementType = "layout",
    printObject = true
)
class LoggingLayout : AbstractStringLayout(StandardCharsets.UTF_8) {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    override fun toSerializable(event: LogEvent): String {
        val timestamp = LocalTime.now().withNano(0).format(timeFormatter)

        val levelColor = when (event.level.name()) {
            "INFO" -> "&f"
            "WARN" -> "&e"
            "ERROR" -> "&c"
            "DEBUG" -> "&b"
            else -> "&f"
        }

        val msg = StringBuilder()

        val messageText = event.message.formattedMessage
            .lineSequence()
            .first()


        msg.append(
            LoggingColor.translate(
                "&7$timestamp &8| $levelColor${event.level}&8: &7$messageText\n"
            )
        )

        val throwable = event.thrown ?: event.thrownProxy?.throwable

        if (throwable != null && !messageText.contains(throwable::class.java.name)) {
            appendThrowable(throwable, msg)
        }

        return msg.toString()
    }


    private fun appendThrowable(t: Throwable, msg: StringBuilder) {
        msg.append(
            LoggingColor.translate(
                "&c${t::class.java.name}: ${t.message}\n"
            )
        )

        t.stackTrace.forEach { element ->
            msg.append(
                LoggingColor.translate(
                    "&7\tat ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})\n"
                )
            )
        }

        t.cause?.let { cause ->
            msg.append(LoggingColor.translate("&7Caused by:\n"))
            appendThrowable(cause, msg)
        }
    }

    companion object {
        @JvmStatic
        @PluginFactory
        fun createLayout(): LoggingLayout = LoggingLayout()
    }
}
