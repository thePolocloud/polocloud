package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands;

abstract class Command(val name: String, val description: String, vararg val aliases: String) {

    var  defaultExecution: CommandExecution? = null
    val commandSyntaxes: MutableList<CommandSyntax> = ArrayList()

    fun syntax(execution: CommandExecution, vararg arguments: CommandArgument<*>) {
        this.commandSyntaxes.add(CommandSyntax(execution, null, arguments))
    }

    fun commandSyntaxes(): MutableList<CommandSyntax> {
        return this.commandSyntaxes
    }

    fun syntax(execution: CommandExecution, description: String, vararg arguments: CommandArgument<*>) {
        this.commandSyntaxes.add(CommandSyntax(execution, description, arguments))
    }

    fun defaultExecution(execution: CommandExecution) {
        this.defaultExecution = execution
    }

    fun hasSyntaxCommands(): Boolean {
        return !this.commandSyntaxes.isEmpty()
    }

    override fun equals(other: Any?): Boolean {
        return other is Command && other.name == name
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + (defaultExecution?.hashCode() ?: 0)
        result = 31 * result + commandSyntaxes.hashCode()
        return result
    }
}