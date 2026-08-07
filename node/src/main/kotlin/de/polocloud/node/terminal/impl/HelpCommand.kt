package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.Command
import de.polocloud.node.terminal.CliTerminal
import de.polocloud.node.terminal.CommandOutput.white
import org.slf4j.LoggerFactory

/**
 * Lists every registered terminal command with its aliases and description.
 */
class HelpCommand(
    private val terminal: CliTerminal,
) : Command("help", "Lists all available commands", "h", "?") {

    private val logger = LoggerFactory.getLogger(HelpCommand::class.java)

    init {
        defaultExecution {
            logger.info("Available commands:")
            terminal.commandService.commands.sortedBy { it.name }.forEach { command ->
                val aliases = command.aliases.joinToString(", ")
                if (aliases.isNotEmpty()) {
                    logger.info("  ${white(command.name)} &8(&7$aliases&8)&r: ${command.description}")
                } else {
                    logger.info("  ${white(command.name)}&r: ${command.description}")
                }
            }
        }
    }
}
