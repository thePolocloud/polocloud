package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl

import com.sun.management.OperatingSystemMXBean
import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.i18n
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.Command
import java.lang.management.ManagementFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import kotlin.math.roundToInt

class InfoCommand : Command("info", "Used to display information about the agent") {

    val osBean: OperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean

    init {

        // call cpuUsage() to ensure the bean is initialized -> this is necessary because the bean might not be initialized yet when the command is executed
        cpuUsage()

        defaultExecution {
            i18n.info("agent.terminal.command.info")
            i18n.info("agent.terminal.command.info.line.1", formatDuration(System.currentTimeMillis() - System.getProperty("polocloud.lifecycle.boot-time").toLong()))
            i18n.info("agent.terminal.command.info.line.2", Agent.runtime.javaClass.simpleName)
            i18n.info("agent.terminal.command.info.line.3", System.getProperty("java.version"))
            i18n.info("agent.terminal.command.info.line.4", cpuUsage(), "%")
            i18n.info("agent.terminal.command.info.line.5", usedMemory())
            i18n.info("agent.terminal.command.info.line.6", maxMemory())
            i18n.info("agent.terminal.command.info.line.7", Agent.eventService.registeredAmount())
        }
    }

    private fun cpuUsage(): Double {
        val load = osBean.cpuLoad

        if (load < 0) {
            return -1.0
        }

        return (load * 10000.0).roundToInt() / 100.0
    }

    private fun usedMemory(): Double {
        val runtime = Runtime.getRuntime()
        val usedBytes = runtime.totalMemory() - runtime.freeMemory()

        return calculateMemory(usedBytes)
    }

    private fun maxMemory(): Double {
        val runtime = Runtime.getRuntime()
        val maxBytes = runtime.maxMemory()

        return calculateMemory(maxBytes)
    }

    private fun calculateMemory(bytes: Long): Double {
        val mb = bytes / 1024.0 / 1024.0
        return BigDecimal(mb)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

     fun formatDuration(millis: Long): String {
        var duration = Duration.ofMillis(millis)

        val days = duration.toDays()
        duration = duration.minusDays(days)

        val hours = duration.toHours()
        duration = duration.minusHours(hours)

        val minutes = duration.toMinutes()
        duration = duration.minusMinutes(minutes)

        val seconds = duration.seconds
        val sb = StringBuilder()

        if (days > 0) sb.append(days).append("d ")
        if (hours > 0 || days > 0) sb.append(hours).append("h ")
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m ")
        if (seconds > 0 || minutes > 0 || hours > 0 || days > 0) sb.append(seconds).append("s ")

        return sb.toString()
    }
}