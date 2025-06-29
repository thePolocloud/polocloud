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
import dev.httpmarco.polocloud.platforms.PlatformIndex

class GroupCommand(private val groupStorage: RuntimeGroupStorage) : Command("group", "Manage all group actions") {

    init {
        syntax(execution = { context ->
            if (groupStorage.items().isEmpty()) {
                logger.info("No groups found.")
                return@syntax
            }
            logger.info("Found ${groupStorage.items().size} groups&8:")
            groupStorage.items()
                .forEach { logger.info(" &8- &3${it.data.name} &8(&7minOnlineServices&8=&7${it.data.minOnlineService} services&8=&7${it.serviceCount()}&8)") }
        }, KeywordArgument("list"))

        val groupArgument = GroupArgument()

        syntax(execution = { context ->
            groupStorage.destroy(context.arg(groupArgument))
            logger.info("Group ${context.arg(groupArgument).data.name} deleted.")
        }, groupArgument, KeywordArgument("delete"))

        syntax(execution = { context ->
            var group = context.arg(groupArgument)

            logger.info("Group &3${group.data.name}&8:")
            logger.info(" &8- &7Min Memory&8: &f${group.data.minMemory}MB")
            logger.info(" &8- &7Max Memory&8: &f${group.data.maxMemory}MB")
            logger.info(" &8- &7Min Online Services&8: &f${group.data.minOnlineService}")
            logger.info(" &8- &7Max Online Services&8: &f${group.data.maxOnlineService}")
            logger.info(" &8- &7Online services&8: &f${group.serviceCount()}")
            logger.info(" &8- &7Platform&8: &f${group.data.platform.group} (${group.data.platform.version})")
        }, groupArgument, KeywordArgument("info"))


        val nameArgument = TextArgument("name")
        val minOnlineServices = IntArgument("minOnlineServices")
        val maxOnlineServices = IntArgument("maxOnlineServices")
        val minMemory = IntArgument("minMemory")
        val maxMemory = IntArgument("maxMemory")

        syntax(execution = { context ->
            Agent.instance.runtime.groupStorage().publish(
                Group(
                    GroupData(
                        context.arg(nameArgument),
                        PlatformIndex("paper", "1.21.6"),
                        context.arg(minMemory),
                        context.arg(maxMemory),
                        context.arg(minOnlineServices),
                        context.arg(maxOnlineServices)
                    )
                )
            )
            logger.info("Group &f${context.arg(nameArgument)} successfully created&8.")
        }, KeywordArgument("create"), nameArgument, minMemory, maxMemory, minOnlineServices, maxOnlineServices)

        syntax(execution = { context ->
            var editType = context.arg(GroupEditFlagArgument())
            var group = context.arg(groupArgument)
            var value = context.arg(TextArgument("value"))

            when (editType) {
                GroupEditFlagArgument.TYPES.MIN_ONLINE_SERVICES -> group.data.minOnlineService = value.toInt()
                GroupEditFlagArgument.TYPES.MAX_ONLINE_SERVICES -> group.data.maxOnlineService = value.toInt()
            }

            group.update()
            logger.info("The group &f${group.data.name} &7has been edited&8: &7Update &3${editType.name} &7to &f$value&8.")
        }, groupArgument, KeywordArgument("edit"), GroupEditFlagArgument(), TextArgument("value"))
    }
}