package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.Command
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.KeywordArgument

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
    }
}