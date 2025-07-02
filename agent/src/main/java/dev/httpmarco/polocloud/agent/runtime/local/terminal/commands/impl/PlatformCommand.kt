package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.Command
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.KeywordArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.PlatformArgument

class PlatformCommand() : Command("platform", "Manage the platforms") {

    init {
        syntax(execution = { context ->
            val platformPool = Agent.instance.platformPool

            if (platformPool.platforms.isEmpty()) {
                logger.info("No platform found.")
                return@syntax
            }
            logger.info("Found ${platformPool.platforms.size} platforms&8:")
            platformPool.platforms.forEach { logger.info(" &8- &3${it.name} &8(&7versions&8=&7${it.versions.size} language&8=&7${it.language.name}&8)") }
        }, KeywordArgument("list"))


        var platformArg = PlatformArgument()
        syntax(execution = {

            var platform = it.arg(platformArg)

            logger.info("Service &3${platform.name}&8:")
            logger.info(" &8- &7Language&8: &f${platform.language}")
            logger.info(" &8- &7Url&8: &f${platform.url}")
            logger.info(" &8- &7Type&8: &f${platform.type}")
            logger.info(" &8- &7Versions&8(&7${platform.versions.size}&8):")

            platform.versions.forEach { version ->
                logger.info("   &8- &7${version.version} &8(&7${version.buildId}&8)")
            }
        }, platformArg)

    }
}