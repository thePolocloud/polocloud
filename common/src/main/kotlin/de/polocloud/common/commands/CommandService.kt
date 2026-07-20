package de.polocloud.common.commands

import java.util.Arrays

class CommandService {

    val commands = ArrayList<Command>()
    val parser = CommandParser(this)

    fun commandsByName(name: String): MutableList<Command> {
        return commands.stream().filter {
            it!!.name.equals(name, ignoreCase = true) || Arrays.stream(it.aliases).anyMatch({ s -> s.equals(name, ignoreCase = true) })
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