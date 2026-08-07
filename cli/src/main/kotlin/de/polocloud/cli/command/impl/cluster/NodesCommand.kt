package de.polocloud.cli.command.impl.cluster

import de.polocloud.cli.Cli
import de.polocloud.cli.command.Command
import de.polocloud.cli.command.arguments.type.KeywordArgument
import de.polocloud.cli.communication.connection.CliConnectionManager
import de.polocloud.cli.logger
import de.polocloud.i18n.api.TranslationService
import kotlinx.coroutines.runBlocking

class NodesCommand(
    private val connectionManager: CliConnectionManager
) : Command("nodes", "cli.command.impl.nodes.description") {

    init {
        syntax(
            {
                if (!connectionManager.isConnected) {
                    logger.info(TranslationService.tr("cli", "cli.connect.notConnected"))
                    return@syntax
                }

                runBlocking {
                    val nodes = Cli.session.clusterClient.listNodes()

                    if (nodes.isEmpty()) {
                        logger.info(TranslationService.tr("cli", "cli.command.impl.nodes.empty"))
                        return@runBlocking
                    }

                    logger.info(TranslationService.tr("cli", "cli.command.impl.nodes.header"))
                    nodes.forEach { node ->
                        logger.info("${node.groupName}-${node.index} (${node.hostname}:${node.port}) [${node.state}]")
                    }
                }
            },
            KeywordArgument("list")
        )
    }
}