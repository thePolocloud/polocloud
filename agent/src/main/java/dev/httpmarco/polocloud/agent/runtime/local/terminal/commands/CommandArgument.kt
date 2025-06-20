package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands

abstract class CommandArgument<T>(open val key: String) {

    open fun defaultArgs(context: CommandContext): MutableList<String> {
        return mutableListOf()
    }

    // if one argument must be a special type
    open fun predication(rawInput: String): Boolean {
        return !(rawInput.startsWith("<") && rawInput.endsWith(">"))
    }

    open fun wrongReason(): String {
     //   return PolocloudSuite.instance().translation().get("terminal.command.argument.wrong")
        return "Wrong argument format"
    }

    abstract fun buildResult(input: String): T
}