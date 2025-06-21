package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands

fun interface CommandExecution {
    fun execute(commandContext: CommandContext)
}