package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl

import dev.httpmarco.polocloud.agent.i18n
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.PlayerArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.Command

class PlayersCommand : Command("players", "Manage the players") {

    init {
        val playerArgument = PlayerArgument()


        syntax({

            val player = it.arg(playerArgument)

            i18n.info("agent.terminal.command.player.info.header", player.name)
            i18n.info("agent.terminal.command.player.info.uniqueId", player.uniqueId)
            i18n.info("agent.terminal.command.player.info.currentProxy", player.currentProxyName)
            i18n.info("agent.terminal.command.player.info.currentServer", player.currentServerName)
        }, playerArgument);
    }
}