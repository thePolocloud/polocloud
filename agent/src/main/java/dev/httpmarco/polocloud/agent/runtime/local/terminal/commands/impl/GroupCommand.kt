package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.groups.GroupData
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.RuntimeGroupStorage
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.Command
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.GroupArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.KeywordArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.TextArgument

class GroupCommand(private val groupStorage: RuntimeGroupStorage) : Command("group", "Manage all group actions") {

    init {
        syntax(execution = { context ->

            if (groupStorage.items().isEmpty()) {
                logger.info("No groups found.")
                return@syntax
            }

            groupStorage.items().forEach { logger.info(" - ${it.data.name}") }
        }, KeywordArgument("list"))

        var groupArgument = GroupArgument()

        var nameArgument = TextArgument("name")

        syntax(execution = { context ->
                Agent.instance.runtime.groupStorage().publish(Group(GroupData(context.arg(nameArgument))))
        }, KeywordArgument("create"), nameArgument)

            syntax(execution = { context -> {
                Agent.instance.runtime.groupStorage().destroy(context.arg(groupArgument))
            logger.info("Group ${context.arg(groupArgument).data.name} deleted.")
        } }, KeywordArgument("delete"), groupArgument)
    }
}