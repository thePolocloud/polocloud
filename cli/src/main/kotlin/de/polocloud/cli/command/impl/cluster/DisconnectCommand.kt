package de.polocloud.cli.command.impl.cluster

import de.polocloud.cli.Cli
import de.polocloud.cli.command.Command
import de.polocloud.cli.communication.connection.CliConnectionManager
import de.polocloud.cli.logger
import de.polocloud.i18n.api.TranslationService

class DisconnectCommand(
    private val connectionManager: CliConnectionManager
) : Command("disconnect", "cli.command.impl.disconnect.description") {

    init {
        defaultExecution {
            if (!connectionManager.isConnected) {
                logger.info(TranslationService.tr("cli", "cli.connect.notConnected"))
                return@defaultExecution
            }

            if (!Cli.terminal.confirm(TranslationService.tr("cli", "cli.command.impl.disconnect.confirmation"))) {
                logger.info(TranslationService.tr("cli", "cli.command.impl.disconnect.cancelled"))
                return@defaultExecution
            }

            connectionManager.disconnect()
            connectionManager.certificateStorage.clearCertificates()

            Cli.terminal.disconnectPrompt()
        }
    }
}