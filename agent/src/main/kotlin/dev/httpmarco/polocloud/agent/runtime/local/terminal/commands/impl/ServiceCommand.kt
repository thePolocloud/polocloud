package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.i18n
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.RuntimeServiceStorage
import dev.httpmarco.polocloud.agent.runtime.local.terminal.JLine3Terminal
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.Command
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.KeywordArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.ServiceArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.StringArrayArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.TextArgument
import kotlin.collections.component1
import kotlin.collections.component2

class ServiceCommand(private val serviceStorage: RuntimeServiceStorage, terminal: JLine3Terminal) :
    Command("service", "Used to manage services", "ser") {

    init {
        syntax(execution = {
            if (serviceStorage.items().isEmpty()) {
                i18n.info("agent.terminal.command.service.not-found")
                return@syntax
            }
            i18n.info("agent.terminal.command.service.found", serviceStorage.items().size)
            serviceStorage.items().forEach {
                i18n.info(
                    "agent.terminal.command.service.list",
                    it.name(),
                    it.state.name,
                    it.hostname,
                    it.port,
                    it.properties.size
                )
            }
        }, KeywordArgument("list"))

        val serviceArgument = ServiceArgument()

        syntax(execution = {
            val service = it.arg(serviceArgument)

            i18n.info("agent.terminal.command.service.info.header", service.name())
            i18n.info("agent.terminal.command.service.info.line.1", service.state)
            i18n.info("agent.terminal.command.service.info.line.2", service.group.data.name)
            i18n.info("agent.terminal.command.service.info.line.3", service.port)
            i18n.info("agent.terminal.command.service.info.line.4", service.hostname)
            i18n.info("agent.terminal.command.service.info.line.5", service.playerCount, service.maxPlayerCount)
            i18n.info("agent.terminal.command.service.info.line.6")

            service.properties.forEach { (key, value) ->
                logger.info("   &8- &7$key&8: &f$value")
            }
        }, serviceArgument)

        syntax(execution = {
            val service = it.arg(serviceArgument)

            service.shutdown()
        }, serviceArgument, KeywordArgument("shutdown"))

        syntax(execution = {
            val service = it.arg(serviceArgument)

            service.logs().forEach { log ->
                println(log)
            }
        }, serviceArgument, KeywordArgument("logs"))

        var templateName = TextArgument("template")
        syntax(execution = {
            val service = it.arg(serviceArgument)
            val template = it.arg(templateName)

            i18n.info("agent.terminal.command.service.export.start", service.name(), template)
            Agent.runtime.templates().saveTemplate(template, service)
            i18n.info("agent.terminal.command.service.export.end", template)
        }, serviceArgument, KeywordArgument("copy"), templateName)

        val commandArg = StringArrayArgument("command")
        syntax(execution = {
            val service = it.arg(serviceArgument)

            Agent.runtime.expender().executeCommand(service, it.arg(commandArg))
            i18n.info("agent.terminal.command.service.execute", service.name(), it.arg(commandArg))
        }, serviceArgument, KeywordArgument("execute"), commandArg)

        syntax(execution = {
            val service = it.arg(serviceArgument)
            terminal.screenService.screenRecordingOf(service)
        }, serviceArgument, KeywordArgument("screen"))
    }
}