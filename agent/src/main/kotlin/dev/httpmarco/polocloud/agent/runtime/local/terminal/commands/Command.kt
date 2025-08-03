package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands

import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.TerminalArgument

abstract class Command(val name: String, val description: String, vararg val aliases: String) {

    var  defaultExecution: CommandExecution? = null
    val commandSyntaxes = ArrayList<CommandSyntax>()

    fun syntax(execution: CommandExecution, vararg arguments: TerminalArgument<*>) {
        this.commandSyntaxes.add(CommandSyntax(execution, null, arguments.toList() as MutableList<TerminalArgument<*>>))
    }

    fun syntax(execution: CommandExecution, description: String, vararg arguments: TerminalArgument<*>) {
        this.commandSyntaxes.add(CommandSyntax(execution, description, arguments.toList() as MutableList<TerminalArgument<*>>))
    }

    fun defaultExecution(execution: CommandExecution) {
        this.defaultExecution = execution
    }

    fun hasSyntaxCommands(): Boolean {
        return this.commandSyntaxes.isNotEmpty()
    }
}