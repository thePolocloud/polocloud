package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type

import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandContext

class StringArrayArgument(key: String) : CommandArgument<String>(key) {

    override fun buildResult(input: String, context: CommandContext): String {
        return input
    }
}