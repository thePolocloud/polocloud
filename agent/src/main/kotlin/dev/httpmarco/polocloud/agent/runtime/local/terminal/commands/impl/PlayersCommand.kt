package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.i18n
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.KeywordArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.PlayerArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.ServiceArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.StringArrayArgument
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


        val reason = StringArrayArgument("reason")
        syntax({
            val player = it.arg(playerArgument)
            val reason = it.arg(reason)
            val response = Agent.playerProvider().kickPlayer(player.uniqueId, reason)

            if (response.success) {
                logger.info("Kicked player ${player.name} for reason: $reason")
            } else {
                logger.warn("Failed to kick player ${player.name}: ${response.errorMessage}")
            }

        }, playerArgument, KeywordArgument("kick"), reason)

        val targetServer = ServiceArgument("server")
        syntax({
            val player = it.arg(playerArgument)
            val server = it.arg(targetServer)
            val response = Agent.playerProvider().connectPlayerToService(player.uniqueId, server)

            if (response.success) {
                logger.info("Send player ${player.name} to service: $reason")
            } else {
                logger.warn("Failed to send player ${player.name}: ${response.errorMessage}")
            }

        }, playerArgument, KeywordArgument("connect"), targetServer)


        val message = StringArrayArgument("message")
        syntax({
            val player = it.arg(playerArgument)
            val targetMessage = it.arg(message)
            val response = Agent.playerProvider().messagePlayer(player.uniqueId, targetMessage)

            if (response.success) {
                logger.info("Send message to player ${player.name}: $reason")
            } else {
                logger.warn("Failed to send player ${player.name} message: ${response.errorMessage}")
            }

        }, playerArgument, KeywordArgument("message"), message)

    }
}