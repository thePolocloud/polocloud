package de.polocloud.cli.command.impl

import de.polocloud.cli.Cli
import de.polocloud.cli.command.Command
import de.polocloud.cli.exitPolocloud
import de.polocloud.cli.logger
import de.polocloud.i18n.api.TranslationService

/**
 * Shuts down the PoloCloud CLI application.
 *
 * Registered with both the primary name `shutdown` and the alias `stop`.
 * Asks for confirmation before delegating to [exitPolocloud] for a clean shutdown sequence.
 */
class ShutdownCommand : Command("shutdown","cli.command.impl.shutdown.description", "stop") {

    init {
        defaultExecution {
            if (Cli.terminal.confirm(TranslationService.tr("cli", "cli.command.impl.shutdown.confirmation"))) {
                exitPolocloud()
            } else {
                logger.info(TranslationService.tr("cli", "cli.command.impl.shutdown.cancelled"))
            }
        }
    }
}
