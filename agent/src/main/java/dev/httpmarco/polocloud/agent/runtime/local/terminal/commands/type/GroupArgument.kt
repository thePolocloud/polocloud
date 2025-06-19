package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandArgument

class GroupArgument : CommandArgument<Group>("group") {

    override fun buildResult(input: String): Group {
        return Agent.instance.runtime.groupStorage().item(input)!!
    }
}