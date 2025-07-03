package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.RuntimeExpender
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter

class LocalRuntimeExpender : RuntimeExpender<LocalService> {

    override fun executeCommand(service: LocalService, command: String) {
        if (service.process == null || command.isEmpty()) {
            logger.warn("Cannot execute command on service ${service.name()}: Process is null or command is empty")
            return
        }

        try {
            val writer = BufferedWriter(OutputStreamWriter(service.process!!.outputStream))
            writer.write(command)
            writer.newLine()
            writer.flush()
        } catch (e: IOException) {
            logger.throwable(e)
        }
    }

    override fun readLogs(service: LocalService, lines: Int): List<String> {
        TODO("Not yet implemented")
    }
}