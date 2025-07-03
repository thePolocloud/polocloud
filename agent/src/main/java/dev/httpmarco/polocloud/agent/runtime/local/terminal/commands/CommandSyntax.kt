package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands

import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.KeywordArgument
import java.util.Arrays

class CommandSyntax(
    val execution: CommandExecution,
    val description: String?,
    val arguments: MutableList<CommandArgument<*>>
) {
    fun usage(): String {
        return java.lang.String.join(
            " ", arguments.stream()
                .map { if (it is KeywordArgument) "&f" + it.key else "&8<&f" + it.key + "&8>" }
                .toList()
        ) + (if (description == null) "" else " &8- &7$description")
    }
}