package dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.TerminalArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.InputContext
import dev.httpmarco.polocloud.agent.services.Service

class ServiceArgument(key: String = "service") : TerminalArgument<Service>(key) {

    override fun buildResult(input: String, context: InputContext): Service {
        // null check is done in the predication method
        return Agent.runtime.serviceStorage().findService(input)!!
    }

    override fun defaultArgs(context: InputContext): MutableList<String> {
        return Agent.runtime.serviceStorage().items().stream().map { it.name() }.toList()
    }

    override fun predication(rawInput: String): Boolean {
        return Agent.runtime.serviceStorage().findService(rawInput) != null
    }
}