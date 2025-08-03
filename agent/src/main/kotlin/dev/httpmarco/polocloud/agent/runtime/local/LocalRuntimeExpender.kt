package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.i18n
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.RuntimeExpender
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter

class LocalRuntimeExpender : RuntimeExpender<LocalService> {

    override fun executeCommand(service: LocalService, command: String): Boolean {
        val process = service.process

        if (process == null || command.isBlank()) {
            i18n.warn("agent.local-runtime.expender.execute.failed", service.name())
            return false
        }

        if (!process.isAlive) {
            return false
        }

        try {
            val writer = BufferedWriter(OutputStreamWriter(process.outputStream))
            writer.write(command)
            writer.newLine()
            writer.flush()
            return true
        } catch (e: IOException) {
            i18n.warn("agent.local-runtime.expender.execute.error")
            logger.throwable(e)
            return false
        }
    }

    override fun readLogs(service: LocalService, lines: Int): List<String> {
        if (lines == -1) {
            return service.logs()
        }

        return service.logs().takeLast(lines)
    }
}