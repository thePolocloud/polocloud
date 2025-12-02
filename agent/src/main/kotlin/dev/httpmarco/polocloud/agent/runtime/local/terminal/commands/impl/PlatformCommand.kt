package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl

import dev.httpmarco.polocloud.agent.i18n
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.Command
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.KeywordArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.PlatformArgument
import dev.httpmarco.polocloud.platforms.PlatformPool

class PlatformCommand() : Command("platform", "Manage the platforms") {

    init {
        syntax(execution = {
            if (PlatformPool.platforms().isEmpty()) {
                i18n.info("agent.terminal.command.platform.not-found")
                return@syntax
            }
            i18n.info("agent.terminal.command.platform.found", PlatformPool.size())
            PlatformPool.platforms().forEach { i18n.info("agent.terminal.command.platform.list", it.name, it.versions.size, it.language.name) }
        }, KeywordArgument("list"))


        val platformArg = PlatformArgument()
        syntax(execution = {

            val platform = it.arg(platformArg)

            i18n.info("agent.terminal.command.platform.info.header", platform.name)

            var bridge = platform.bridge
            if(bridge!= null) {
                i18n.info("agent.terminal.command.platform.info.bridge", bridge.id, bridge.type().name)
            }

            i18n.info("agent.terminal.command.platform.info.line.1", platform.language)
            i18n.info("agent.terminal.command.platform.info.line.2", platform.url)
            i18n.info("agent.terminal.command.platform.info.line.3", platform.type)
            i18n.info("agent.terminal.command.platform.info.line.4", platform.arguments.size)

            platform.arguments.forEach { arg ->
                logger.info("   &8- &7${arg}")
            }

            i18n.info("agent.terminal.command.platform.info.line.5", platform.tasks().size)
            platform.tasks().forEach { task ->
                logger.info("   &8- &7${task.name}")
            }

            i18n.info("agent.terminal.command.platform.info.line.6", platform.versions.size)
            platform.versions.forEach { version ->
                logger.info("   &8- &7${version.version} &8(&7${version.buildId}&8)")
            }
        }, platformArg)

        syntax(execution = {
            TODO()
        }, KeywordArgument("tasks"))

    }
}