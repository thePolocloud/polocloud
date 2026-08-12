package de.polocloud.node.terminal.types

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.TerminalArgument
import de.polocloud.node.player.CloudPlayer
import de.polocloud.node.player.CloudPlayerRepository

/**
 * Terminal argument that resolves a currently-connected [CloudPlayer] by name.
 *
 * The raw input is the player name. The argument only matches when a connected player
 * has that name and offers all connected players' names as tab-completion suggestions.
 */
class PlayerArgument(key: String) : TerminalArgument<CloudPlayer>(key) {

    override fun defaultArgs(context: InputContext): MutableList<String> {
        return CloudPlayerRepository.findAll().map { it.name }.toMutableList()
    }

    override fun predication(rawInput: String): Boolean {
        return rawInput.isNotBlank() && CloudPlayerRepository.findByName(rawInput) != null
    }

    override fun wrongReason(rawInput: String): String {
        return "node.command.player.notExists"
    }

    override fun buildResult(input: String, context: InputContext): CloudPlayer {
        // safe: predication guarantees a matching player exists
        return CloudPlayerRepository.findByName(input)!!
    }
}
