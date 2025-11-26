package dev.httpmarco.polocloud.agent.runtime.local.tracking

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.runtime.abstracts.Track
import dev.httpmarco.polocloud.agent.runtime.local.LocalRuntime
import dev.httpmarco.polocloud.agent.runtime.local.LocalService
import dev.httpmarco.polocloud.shared.events.definitions.service.ServiceLogEvent
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.LinkedList

class ServiceLogTrack(private val service: LocalService) : Track() {

    val cachedLogs = LinkedList<String>()

    override fun start() {
        val process = service.process ?: return

        fun handleStream(input: InputStream) {
            val reader = BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8))

            reader.useLines { lines ->
                lines.forEach { line ->
                    cachedLogs += line

                    val runtime = Agent.runtime
                    if (runtime !is LocalRuntime) return@forEach

                    Agent.eventProvider().call(ServiceLogEvent(this.service, line))

                    val screenService = runtime.terminal.screenService
                    if (!screenService.isServiceRecoding(service)) {
                        return@forEach
                    }

                    screenService.terminal.displayApproved(line)
                }
            }
        }

        this.threads += Thread.startVirtualThread {
            handleStream(process.inputStream)
        }

        this.threads += Thread.startVirtualThread {
            handleStream(process.errorStream)
        }
    }
}