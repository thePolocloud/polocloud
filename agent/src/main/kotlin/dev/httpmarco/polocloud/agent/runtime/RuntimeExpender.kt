package dev.httpmarco.polocloud.agent.runtime

import dev.httpmarco.polocloud.agent.services.Service

interface RuntimeExpender<out S : Service> {

    fun executeCommand(service: @UnsafeVariance S, command: String): Boolean

    fun readLogs(service: @UnsafeVariance S, lines: Int = 100): List<String>

}