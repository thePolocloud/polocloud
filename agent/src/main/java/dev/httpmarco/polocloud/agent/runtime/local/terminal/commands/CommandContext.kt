package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands

class CommandContext {
    private val contexts = HashMap<String, Any?>()

    fun <T> arg(argument: CommandArgument<T>): T {
        return contexts[argument.key] as T
    }

    fun append(argument: CommandArgument<*>, value: Any?) {
        this.contexts.put(argument.key, value)
    }

    fun <T> arg(id: String?): T? {
        return contexts[id] as T?
    }
}