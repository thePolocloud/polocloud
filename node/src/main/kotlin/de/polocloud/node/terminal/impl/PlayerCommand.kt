package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.Command
import de.polocloud.common.commands.type.KeywordArgument
import de.polocloud.common.commands.type.StringArrayArgument
import de.polocloud.node.event.ClusterEventService
import de.polocloud.node.player.CloudPlayer
import de.polocloud.node.player.CloudPlayerRepository
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.terminal.CommandOutput.white
import de.polocloud.node.terminal.types.PlayerArgument
import de.polocloud.node.terminal.types.ServiceArgument
import de.polocloud.shared.event.player.PlayerKickRequestEvent
import de.polocloud.shared.event.player.PlayerSendRequestEvent
import org.slf4j.LoggerFactory

/**
 * Terminal command for inspecting and controlling currently-connected players.
 *
 * `players` (default) / `players list` / `players info <name>` read directly from
 * [CloudPlayerRepository] — no cross-node gRPC fan-out is needed (unlike
 * [ServiceCommand]'s `list`/`info`): every node shares the same database, and a
 * player's row is always current as of its last join/switch/disconnect/kick RPC (see
 * `node/.../player/CloudPlayer.kt`'s persistence design doc).
 *
 * `players send`/`players kick` broadcast a request event to every bridge instance in
 * the cluster instead of a targeted RPC — there is no direct channel from the node into
 * a specific running proxy process — so only the proxy actually hosting the target
 * player acts on it. See [PlayerSendRequestEvent]/[PlayerKickRequestEvent].
 */
class PlayerCommand(
    private val serviceProvider: ServiceProvider,
) : Command("players", "View and control currently-connected players", "player") {

    private val logger = LoggerFactory.getLogger(PlayerCommand::class.java)

    private val playerArgument = PlayerArgument("player")
    private val serverArgument = ServiceArgument("server", serviceProvider)
    private val reasonArgument = StringArrayArgument("reason")

    init {
        defaultExecution { list() }

        syntax({
            list()
        }, "List all connected players", KeywordArgument("list"))

        syntax({ context ->
            info(context.arg(playerArgument))
        }, "Show detailed information about a connected player", playerArgument)

        syntax({ context ->
            send(context.arg(playerArgument), context.arg(serverArgument).name())
        }, "Send a player to another server", playerArgument, KeywordArgument("send"), serverArgument)

        syntax({ context ->
            kick(context.arg(playerArgument), "")
        }, "Kick a connected player", playerArgument, KeywordArgument("kick"))

        syntax({ context ->
            kick(context.arg(playerArgument), context.arg(reasonArgument))
        }, "Kick a connected player with a reason", playerArgument, KeywordArgument("kick"), reasonArgument)
    }

    private fun list() {
        val players = CloudPlayerRepository.findAll()
        if (players.isEmpty()) {
            logger.info("There are no connected players.")
            return
        }
        logger.info("Players (${players.size}):")
        players.sortedBy { it.name }.forEach { player ->
            logger.info("  ${player.name} &8|&r proxy: ${player.currentProxy} &8|&r server: ${player.currentServer ?: "-"}")
        }
    }

    private fun info(player: CloudPlayer) {
        logger.info("Player ${player.name}:")
        logger.info("  id: ${white(player.id.toString())}")
        logger.info("  proxy: ${white(player.currentProxy)}")
        logger.info("  server: ${white(player.currentServer ?: "-")}")
        val properties = player.properties.entries.joinToString { (key, value) -> "$key=$value" }
        logger.info("  properties: ${white(properties.ifBlank { "-" })}")
    }

    private fun send(player: CloudPlayer, server: String) {
        logger.info("Sending ${player.name} to $server...")
        ClusterEventService.call(PlayerSendRequestEvent(player.id.toString(), server))
    }

    private fun kick(player: CloudPlayer, reason: String) {
        logger.info("Kicking ${player.name}${if (reason.isNotBlank()) " ($reason)" else ""}...")
        ClusterEventService.call(PlayerKickRequestEvent(player.id.toString(), reason))
    }
}
