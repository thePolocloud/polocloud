package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl

import dev.httpmarco.polocloud.agent.runtime.local.terminal.Jline3Terminal
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.Command

class ClearCommand(private val terminal : Jline3Terminal) : Command("clear", "Clears the terminal screen", "cc") {

    init {
        defaultExecution {
            terminal.clearScreen()
        }
    }
}