package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type

import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandContext

class IntArgument(key: String) : CommandArgument<Int>(key) {
    override fun predication(rawInput: String): Boolean {
        try {
            rawInput.toInt()
            return true
        } catch (_: NumberFormatException) {
            return false
        }
    }

    override fun wrongReason(): String {
        return "Input is not a valid integer."
    }

    override fun defaultArgs(context: CommandContext): MutableList<String> {
        return mutableListOf()
    }

    override fun buildResult(input: String): Int {
        return input.toInt()
    }
}
