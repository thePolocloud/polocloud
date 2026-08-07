package de.polocloud.cli.command.impl.cluster

import de.polocloud.cli.Cli
import de.polocloud.cli.command.Command
import de.polocloud.cli.command.arguments.type.KeywordArgument
import de.polocloud.cli.communication.connection.CliConnectionManager
import de.polocloud.cli.logger
import de.polocloud.i18n.api.TranslationService
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId

class ClusterCommand(
    private val connectionManager: CliConnectionManager
) : Command("cluster", "cli.command.impl.cluster.description") {

    init {
        syntax(
            {
                if (!connectionManager.isConnected) {
                    logger.info(TranslationService.tr("cli", "cli.connect.notConnected"))
                    return@syntax
                }

                runBlocking {
                    val token = Cli.session.clusterClient.createToken()

                    val time = Instant.ofEpochMilli(token.expiresAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()

                    logger.info("Token: ${token.token} available until $time")
                }
            },
            KeywordArgument("connect")
        )
    }
}