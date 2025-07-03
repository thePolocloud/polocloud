package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandContext
import dev.httpmarco.polocloud.agent.services.Service

class ServiceArgument(key: String = "service") : CommandArgument<Service>(key) {

    override fun buildResult(input: String, context: CommandContext): Service {
        // null check is done in the predication method
        return Agent.instance.runtime.serviceStorage().findService(input)!!
    }

    override fun defaultArgs(context: CommandContext): MutableList<String> {
        return Agent.instance.runtime.serviceStorage().items().stream().map { it.name() }.toList()
    }

    override fun predication(rawInput: String): Boolean {
        return Agent.instance.runtime.serviceStorage().findService(rawInput) != null
    }
}