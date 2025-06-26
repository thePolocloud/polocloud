package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.groups.GroupData
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.RuntimeGroupStorage
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.Command
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.GroupArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.GroupEditFlagArgument
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
            logger.info("Found ${groupStorage.items().size} groups&8:")
            groupStorage.items().forEach { logger.info(" &8- &3${it.data.name} &8(&7minOnlineServices&8=&7${it.data.minOnlineService}&8)") }
        }, KeywordArgument("list"))

        val groupArgument = GroupArgument()

        syntax(execution = { context ->
            groupStorage.destroy(context.arg(groupArgument))
            logger.info("Group ${context.arg(groupArgument).data.name} deleted.")
        }, groupArgument, KeywordArgument("delete"))

        val nameArgument = TextArgument("name")
        val minOnlineServices = IntArgument("minOnlineServices")
        val maxOnlineServices = IntArgument("maxOnlineServices")

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


        syntax(execution = { context -> {
            Agent.instance.runtime.groupStorage().publish(
                Group(
                    GroupData(
                        context.arg(nameArgument),
                        context.arg(minOnlineServices),
                        context.arg(minOnlineServices)
                    )
                )
            )
            logger.info("Group &f${context.arg(nameArgument)} successfully created&8.")
        }}, KeywordArgument("test1"), nameArgument, minOnlineServices)


        syntax(execution = { context ->
            TODO()
        }, groupArgument, KeywordArgument("edit"), GroupEditFlagArgument(), TextArgument("value"))
    }
}