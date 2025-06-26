package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.groups.GroupData
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.RuntimeGroupStorage
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.Command
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.GroupArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.IntArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.KeywordArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.TextArgument

class GroupCommand(private val groupStorage: RuntimeGroupStorage) : Command("group", "Manage all group actions") {

    init {
        syntax(execution = { context ->
            if (groupStorage.items().isEmpty()) {
                logger.info("No groups found.")
                return@syntax
            }
            groupStorage.items().forEach { logger.info(" - ${it.data.name} &8(&7minOnlineServices&8=&7${it.data.minOnlineService}&8)") }
        }, KeywordArgument("list"))

        var groupArgument = GroupArgument()

        syntax(execution = { context -> {
            logger.info("b")
        }}, groupArgument, KeywordArgument("info"))

        syntax(execution = { context -> {
            logger.info("a")
            groupStorage.destroy(context.arg(groupArgument))
            logger.info("Group ${context.arg(groupArgument).data.name} deleted.")
        }
        }, groupArgument, KeywordArgument("delete"))

        var nameArgument = TextArgument("name")
        var minOnlineServices = IntArgument("minOnlineServices")
        var maxOnlineServices = IntArgument("maxOnlineServices")

        syntax(execution = { context ->
            Agent.instance.runtime.groupStorage().publish(
                Group(
                    GroupData(
                        context.arg(nameArgument),
                        context.arg(minOnlineServices),
                        context.arg(maxOnlineServices)
                    )
                )
            )
            logger.info("Group &f${context.arg(nameArgument)} successfully created&8.")
        }, KeywordArgument("create"), nameArgument, minOnlineServices, maxOnlineServices)
    }
}