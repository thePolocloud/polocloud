package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandContext
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandSyntax

class GroupArgument(key: String = "group") : CommandArgument<Group>(key) {

    override fun buildResult(input: String, context: CommandContext): Group {
        return Agent.instance.runtime.groupStorage().item(input)!!
    }

    override fun defaultArgs(context: CommandContext): MutableList<String> {
        return Agent.instance.runtime.groupStorage().items().map { it.data.name }.toMutableList()
    }

    override fun predication(rawInput: String): Boolean {
        return Agent.instance.runtime.groupStorage().present(rawInput)
    }
}