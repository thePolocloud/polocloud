package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.Command
import de.polocloud.node.terminal.CommandOutput.white
import de.polocloud.updater.UpdateResult
import de.polocloud.updater.Updater
import org.slf4j.LoggerFactory

/**
 * Manually checks for and, if one exists, downloads a newer PoloCloud release, staging
 * it for the next boot. Used when `general.autoUpdate` is disabled, so applying an
 * update is an explicit operator decision. Only stages it — the node keeps running the
 * old version until it is restarted, so the operator can pick a safe moment to do so.
 */
class UpdateCommand : Command("update", "Checks for and downloads a newer PoloCloud version") {

    private val logger = LoggerFactory.getLogger(UpdateCommand::class.java)

    init {
        defaultExecution {
            logger.info("Checking for a newer PoloCloud version...")

            when (val result = Updater.download()) {
                is UpdateResult.Applied -> logger.info(
                    "Downloaded ${white(result.version.toDisplayString())}. Restart the node to apply it."
                )
                UpdateResult.UpToDate -> logger.info("Already running the latest version.")
                is UpdateResult.Failed -> logger.info("Update check failed: ${white(result.reason)}")
            }
        }
    }
}