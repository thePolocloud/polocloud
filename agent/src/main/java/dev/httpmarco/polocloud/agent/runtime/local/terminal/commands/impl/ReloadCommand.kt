package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.Command

class ReloadCommand : Command("reload", "Reloads the agent configuration") {

    init {
        defaultExecution( {
            logger.info("Reloading agent configuration&8...")

            Agent.instance.runtime.groupStorage().reload()

            logger.info("Agent configuration reloaded &3successfully&8.")
        })
    }
}