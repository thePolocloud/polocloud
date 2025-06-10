package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands

interface CommandExecution {
    fun execute(commandContext: CommandContext?)
}