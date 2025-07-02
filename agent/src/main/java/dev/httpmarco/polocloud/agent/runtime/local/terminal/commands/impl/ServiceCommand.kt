package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl

import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.RuntimeServiceStorage
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.Command
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.KeywordArgument

class ServiceCommand(private val serviceStorage: RuntimeServiceStorage) : Command("service", "Used to manage services") {

    init {
        syntax(execution = { context ->
            if (serviceStorage.items().isEmpty()) {
                logger.info("No service found.")
                return@syntax
            }
            logger.info("Found ${serviceStorage.items().size} groups&8:")
            serviceStorage.items().forEach { logger.info(" &8- &3${it.name()} &8(&7uuid&8=&7${it.uniqueId}&8)") }
        }, KeywordArgument("list"))

        syntax(execution = {

        }, )

    }
}