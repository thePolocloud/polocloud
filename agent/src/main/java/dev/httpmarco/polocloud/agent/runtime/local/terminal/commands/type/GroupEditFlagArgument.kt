package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type

import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandContext

class GroupEditFlagArgument(editValue: String = "key") : CommandArgument<GroupEditFlagArgument.TYPES>(editValue) {

    override fun buildResult(input: String): TYPES {
        return TYPES.valueOf(input.uppercase())
    }

    override fun defaultArgs(context: CommandContext): MutableList<String> {
        return TYPES.entries.map { it.name }.toMutableList()
    }

    enum class TYPES {
        MIN_ONLINE_SERVICES,
        MAX_ONLINE_SERVICES,
    }
}