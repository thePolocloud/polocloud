package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands;

abstract class Command(val name: String, val description: String, vararg val aliases: String) {

    var  defaultExecution: CommandExecution? = null
    val commandSyntaxes = ArrayList<CommandSyntax>()

    fun syntax(execution: CommandExecution, vararg arguments: CommandArgument<*>) {
        this.commandSyntaxes.add(CommandSyntax(execution, null, arguments.toList() as MutableList<CommandArgument<*>>))
    }

    fun commandSyntaxes(): MutableList<CommandSyntax> {
        return this.commandSyntaxes
    }

    fun syntax(execution: CommandExecution, description: String, vararg arguments: CommandArgument<*>) {
        this.commandSyntaxes.add(CommandSyntax(execution, description, arguments.toList() as MutableList<CommandArgument<*>>))
    }

    fun defaultExecution(execution: CommandExecution) {
        this.defaultExecution = execution
    }

    fun hasSyntaxCommands(): Boolean {
        return !this.commandSyntaxes.isEmpty()
    }
}