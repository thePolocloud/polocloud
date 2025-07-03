package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.RuntimeServiceStorage
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.Command
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.KeywordArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.ServiceArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.StringArrayArgument

class ServiceCommand(private val serviceStorage: RuntimeServiceStorage) :
    Command("service", "Used to manage services") {

    init {
        syntax(execution = { context ->
            if (serviceStorage.items().isEmpty()) {
                logger.info("No service found.")
                return@syntax
            }
            logger.info("Found ${serviceStorage.items().size} services&8:")
            serviceStorage.items().forEach { logger.info(" &8- &3${it.name()} &8(&7uuid&8=&7${it.uniqueId}&8)") }
        }, KeywordArgument("list"))

        var serviceArgument = ServiceArgument()

        syntax(execution = {
            var service = it.arg(serviceArgument)

            logger.info("Service &3${service.name()}&8:")
            logger.info(" &8- &7Id&8: &f${service.uniqueId}")
            logger.info(" &8- &7State&8: &f${service.state}")
            logger.info(" &8- &7Group&8: &f${service.group.data.name}")
            logger.info(" &8- &7Port&8: &f${service.port}")
            logger.info(" &8- &7Hostname&8: &f${service.hostname}")
        }, serviceArgument)

        syntax(execution = {
            var service = it.arg(serviceArgument)

            service.shutdown()
        }, serviceArgument, KeywordArgument("shutdown"))

        syntax(execution = {
            var service = it.arg(serviceArgument)

            TODO()
        }, serviceArgument, KeywordArgument("logs"))

        var commandArg = StringArrayArgument("command")
        syntax(execution = {
            var service = it.arg(serviceArgument)

            Agent.instance.runtime.expender().executeCommand(service, it.arg(commandArg))
            logger.info("Executed command on service &3${service.name()}&8: &f${it.arg(commandArg)}")
        }, serviceArgument, KeywordArgument("execute"), commandArg)
    }
}