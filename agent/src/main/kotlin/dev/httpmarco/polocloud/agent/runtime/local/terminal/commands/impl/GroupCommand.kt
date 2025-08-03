package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.i18n
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.RuntimeGroupStorage
import dev.httpmarco.polocloud.agent.runtime.local.terminal.JLine3Terminal
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.Command
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.GroupArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.GroupEditFlagArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.IntArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.KeywordArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.TextArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.setup.impl.GroupSetup

class GroupCommand(private val groupStorage: RuntimeGroupStorage, private val terminal: JLine3Terminal) : Command("group", "Manage all group actions") {

    init {
        syntax(execution = {
            if (groupStorage.items().isEmpty()) {
                i18n.info("agent.terminal.command.group.not-found")
                return@syntax
            }
            i18n.info("agent.terminal.command.group.found", groupStorage.items().size)
            groupStorage.items().forEach { i18n.info("agent.terminal.command.group.list", it.data.name, it.data.minOnlineService, it.serviceCount()) }
        }, KeywordArgument("list"))

        val groupArgument = GroupArgument()

        syntax(execution = { context ->
            val group = context.arg(groupArgument)

            i18n.info("agent.terminal.command.group.info.header", group.data.name)
            i18n.info("agent.terminal.command.group.info.line.1", group.data.minMemory)
            i18n.info("agent.terminal.command.group.info.line.2", group.data.maxMemory)
            i18n.info("agent.terminal.command.group.info.line.3", group.data.minOnlineService)
            i18n.info("agent.terminal.command.group.info.line.4", group.data.maxOnlineService)
            i18n.info("agent.terminal.command.group.info.line.5", group.serviceCount())
            i18n.info("agent.terminal.command.group.info.line.6", group.data.platform.group, group.data.platform.version)
            i18n.info("agent.terminal.command.group.info.line.7")

            group.data.properties.forEach { (key, value) ->
                logger.info("   &8- &7$key&8: &f$value")
            }

        }, groupArgument)


        syntax(execution = { context ->
            val editType = context.arg(GroupEditFlagArgument())
            val group = context.arg(groupArgument)

            val stringValue = context.arg(TextArgument("value"))

            fun convertValueToInt(argument: IntArgument): Int? {
                if (!argument.predication(stringValue)) {
                    logger.warn(argument.wrongReason(stringValue))
                    return null
                }
                return argument.buildResult(stringValue, context)
            }

            when (editType) {
                GroupEditFlagArgument.TYPES.MIN_ONLINE_SERVICES -> {
                    val value = convertValueToInt(IntArgument("value", minValue = 0)) ?: return@syntax
                    if (value > group.data.maxOnlineService) {
                        i18n.info("agent.terminal.command.group.edit.warn.above-max", editType)
                        return@syntax
                    }
                    group.data.minOnlineService = value
                }
                GroupEditFlagArgument.TYPES.MAX_ONLINE_SERVICES -> {
                    val value = convertValueToInt(IntArgument("value", minValue = 0)) ?: return@syntax
                    if (value < group.data.minOnlineService) {
                        i18n.info("agent.terminal.command.group.edit.warn.below-min", editType)
                        return@syntax
                    }
                    group.data.maxOnlineService = value
                }
                GroupEditFlagArgument.TYPES.MIN_MEMORY -> {
                    val value = convertValueToInt(IntArgument("value", minValue = 1)) ?: return@syntax
                    if (value > group.data.maxMemory) {
                        i18n.info("agent.terminal.command.group.edit.warn.above-max", editType)
                        return@syntax
                    }
                    group.data.minMemory = value
                }
                GroupEditFlagArgument.TYPES.MAX_MEMORY -> {
                    val value = convertValueToInt(IntArgument("value", minValue = 1)) ?: return@syntax
                    if (value < group.data.minMemory) {
                        i18n.info("agent.terminal.command.group.edit.warn.below-min", editType)
                        return@syntax
                    }
                    group.data.maxMemory = value
                }
            }

            group.update()
            i18n.info("agent.terminal.command.group.edit.successful", group.data.name, editType.name, stringValue)
        }, groupArgument, KeywordArgument("edit"), GroupEditFlagArgument(), TextArgument("value"))

        syntax(execution = { context ->
            val group = context.arg(groupArgument)
            i18n.info("agent.terminal.command.group.shutdown-all")
            group.shutdownAll()
            i18n.info("agent.terminal.command.group.shutdown-all.successful", group.data.name)
        }, groupArgument, KeywordArgument("shutdownAll"))

        syntax(execution = { context ->
            val group = context.arg(groupArgument)

            Agent.runtime.groupStorage().destroy(group)
            group.shutdownAll()

            i18n.info("agent.terminal.command.group.deleted", group.data.name)
        }, groupArgument, KeywordArgument("delete"))

        syntax(execution = {
            terminal.setupController.start(GroupSetup())
        }, KeywordArgument("create"))
    }
}