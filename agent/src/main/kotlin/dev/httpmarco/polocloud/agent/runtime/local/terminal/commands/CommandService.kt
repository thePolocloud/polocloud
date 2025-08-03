package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands

import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl.HelpCommand
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl.InfoCommand
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl.ReloadCommand
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl.ShutdownCommand
import java.util.*

class CommandService {
    val commands = ArrayList<Command>()
    val parser = CommandParser(this)

    init {
        this.registerCommand(ShutdownCommand())
        this.registerCommand(ReloadCommand())
        this.registerCommand(InfoCommand())
        this.registerCommand(HelpCommand(this))
    }

    fun commandsByName(name: String): MutableList<Command> {
        return commands.stream().filter {
            it!!.name == name || Arrays.stream(it.aliases).anyMatch({ s -> s.equals(name) })
        }.toList()
    }

    fun registerCommand(command: Command) {
        this.commands.add(command)
    }

    fun registerCommands(vararg commands: Command) {
        for (command in commands) {
            registerCommand(command)
        }
    }

    fun unregisterCommand(command: Command) {
        this.commands.remove(command)
    }

    fun call(commandId: String, args: Array<String>) {
        parser.parse(commandId, args)
    }
}