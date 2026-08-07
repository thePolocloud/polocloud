package de.polocloud.cli.command.impl.cluster

import de.polocloud.cli.Cli
import de.polocloud.cli.command.Command
import de.polocloud.cli.command.arguments.type.KeywordArgument
import de.polocloud.cli.communication.connection.CliConnectionManager
import de.polocloud.cli.logger
import de.polocloud.i18n.api.TranslationService
import kotlinx.coroutines.runBlocking

class ServicesCommand(
    private val connectionManager: CliConnectionManager
) : Command("services", "cli.command.impl.services.description") {

    init {
        syntax(
            {
                if (!connectionManager.isConnected) {
                    logger.info(TranslationService.tr("cli", "cli.connect.notConnected"))
                    return@syntax
                }

                runBlocking {
                    val services = Cli.session.serviceClient.listServices()

                    if (services.isEmpty()) {
                        logger.info(TranslationService.tr("cli", "cli.command.impl.services.empty"))
                        return@runBlocking
                    }

                    // Resolve each service's nodeId to a human-readable node label (e.g.
                    // "node-0"), mirroring the node terminal's service inspection command.
                    val nodeLabels = Cli.session.clusterClient.listNodes()
                        .associate { it.id to "${it.groupName}-${it.index}" }

                    val rows = services
                        .map { service -> service to (nodeLabels[service.nodeId] ?: service.nodeId.ifBlank { "-" }) }
                        .sortedBy { (_, node) -> node }

                    logger.info(TranslationService.tr("cli", "cli.command.impl.services.header"))
                    rows.forEach { (service, node) ->
                        logger.info(
                            "%-10s %-15s %-36s %-8s %-6s %-8s %s".format(
                                service.state,
                                service.plan,
                                service.uuid,
                                service.boundPort,
                                service.pid,
                                "${service.onlinePlayers}/${service.maxPlayers}",
                                node,
                            )
                        )
                    }
                }
            },
            KeywordArgument("list")
        )
    }
}