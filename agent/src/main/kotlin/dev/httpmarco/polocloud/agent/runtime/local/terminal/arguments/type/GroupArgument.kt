package dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.TerminalArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.InputContext

class GroupArgument(key: String = "group") : TerminalArgument<Group>(key) {

    override fun buildResult(input: String, context: InputContext): Group {
        return Agent.runtime.groupStorage().item(input)!!
    }

    override fun defaultArgs(context: InputContext): MutableList<String> {
        return Agent.runtime.groupStorage().items().map { it.data.name }.toMutableList()
    }

    override fun predication(rawInput: String): Boolean {
        return Agent.runtime.groupStorage().present(rawInput)
    }
}