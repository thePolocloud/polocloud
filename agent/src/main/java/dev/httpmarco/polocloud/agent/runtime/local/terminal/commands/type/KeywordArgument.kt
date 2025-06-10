package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type

import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandContext

class KeywordArgument(key: String?) : CommandArgument<String?>(key) {
    override fun defaultArgs(context: CommandContext?): MutableList<String?> {
        return listOf(key) as MutableList<String?>
    }

    override fun wrongReason(): String {
        return ""
    }

    override fun buildResult(input: String?): String {
        return ""
    }

    override fun predication(rawInput: String): Boolean {
        return super.predication(rawInput) && rawInput.equals(key, ignoreCase = true)
    }
}