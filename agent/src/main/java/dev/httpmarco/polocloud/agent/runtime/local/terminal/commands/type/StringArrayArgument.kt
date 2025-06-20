package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type

import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandArgument

class StringArrayArgument(key: String) : CommandArgument<String>(key) {

    override fun buildResult(input: String): String {
        return input
    }
}