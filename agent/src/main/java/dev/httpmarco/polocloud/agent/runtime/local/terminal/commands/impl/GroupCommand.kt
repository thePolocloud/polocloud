package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl

import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.Command
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.GroupArgument

class GroupCommand : Command("group", "Manage all group actions") {

    init {
        var groupArgument = GroupArgument()

        syntax(
            execution = { context ->
                println("Command executed with context: $context")
            },
            arguments = arrayOf()
        )
    }
}