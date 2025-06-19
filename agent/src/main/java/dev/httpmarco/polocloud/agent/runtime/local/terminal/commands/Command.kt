package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands;

abstract class Command(private val name: String, private val description: String?, vararg aliases: List<String>) {

    private var defaultExecution: CommandExecution? = null
    private val commandSyntaxes: MutableList<CommandSyntax?> = ArrayList()


    fun name(): String {
        return name
    }

    fun syntax(execution: CommandExecution, vararg arguments: CommandArgument<*>) {
        this.commandSyntaxes.add(CommandSyntax(execution, null, arguments))
    }

    fun defaultExecution(): CommandExecution? {
        return this.defaultExecution
    }

    fun commandSyntaxes(): MutableList<CommandSyntax?> {
        return this.commandSyntaxes
    }

    fun syntax(execution: CommandExecution, description: String, vararg arguments: CommandArgument<*>) {
        this.commandSyntaxes.add(CommandSyntax(execution, description, arguments))
    }

    fun defaultExecution(execution: CommandExecution?) {
        this.defaultExecution = execution
    }

    fun hasSyntaxCommands(): Boolean {
        return !this.commandSyntaxes.isEmpty()
    }

    override fun equals(other: Any?): Boolean {
        return other is Command && other.name == name
    }
}