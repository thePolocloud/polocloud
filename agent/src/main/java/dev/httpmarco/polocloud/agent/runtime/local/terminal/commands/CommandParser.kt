package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands

import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.KeywordArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type.StringArrayArgument
import java.util.*
import kotlin.Array
import kotlin.Boolean


object CommandParser {

    fun serializer(commandService: CommandService, name: String, args: Array<String>) {
        // all command with same start name
        val commands = commandService.commandsByName(name)

        if (executeCommand(commands, args)) {
            return
        }

        // we must calculate the usage, because no command was found
        for (command in commands) {
            if (command.defaultExecution != null) {
                command.defaultExecution!!.execute(CommandContext())
            } else {
                for (syntaxCommand in command.commandSyntaxes()) {
                    logger.info("${command.name} ${syntaxCommand.usage()}")
                }
            }
        }
    }

    private fun executeCommand(commands: MutableList<Command>, args: Array<String>): Boolean {
        for (command in commands) {
            if ((!command.hasSyntaxCommands()) || args.isEmpty()) {
                return false
            }

            for (syntaxCommand in command.commandSyntaxes()) {
                if (args.size != syntaxCommand.arguments.size && Arrays.stream(syntaxCommand.arguments).noneMatch { it -> it is StringArrayArgument }) {
                    continue
                }


                println("Found command: ${command.name} with syntax: ${syntaxCommand.usage()}")
                val commandContext = CommandContext()

                var provedSyntax = true
                var provedSyntaxWarning : String? = null

                for (i in 0..<syntaxCommand.arguments.size) {
                    val argument = syntaxCommand.arguments[i]

                    if (i >= args.size) {
                        println("Not enough arguments provided for command: ${command.name}")
                        provedSyntax = false
                        break
                    }

                    val rawInput = args[i]

                    if (argument is StringArrayArgument) {
                        TODO()
                        //commandContext.append(argument, argument.buildResult(String.join(" ", Arrays.copyOfRange(args, i, args.size))))
                        break
                    } else if (argument is KeywordArgument ) {
                        if (argument.key != rawInput) {
                            provedSyntax = false;
                            break;
                        }
                    } else if (!argument.predication(rawInput)) {
                        provedSyntaxWarning = argument.wrongReason()
                        continue
                    }
                        //   println("Adding argument: ${argument.key} with value: $rawInput")
                    commandContext.append(argument, argument.buildResult(rawInput))
                }

                if (!provedSyntax) {
                    continue
                }

                if (provedSyntaxWarning != null) {
                    logger.warn(provedSyntaxWarning)
                    return true
                }

                println("Executing command: ${command.name} with arguments: ${args.contentToString()}")
                println("Command context: $commandContext")
                println("Command syntax: ${syntaxCommand.usage()}")
                syntaxCommand.execution.execute(commandContext)
                return true
            }
        }
        return false
    }
}
