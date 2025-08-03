package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands

import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.InputContext
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.KeywordArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.StringArrayArgument
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.TextArgument
import kotlin.collections.forEach

class CommandParser(private val commandService: CommandService) {

    fun parse(commandId: String, args: Array<String>) {
        val commands = commandService.commandsByName(commandId)

        if (commands.isEmpty()) {
            return
        }

        val command = commands.first()
        val syntax = findSyntaxCommand(command, args)

        if (syntax == null) {
            if (command.defaultExecution != null) {
                command.defaultExecution!!.execute(InputContext())
            } else {
                command.commandSyntaxes.forEach { syntax ->
                    val commandLayout = StringBuilder()

                    syntax.arguments.forEach {
                        if (it is KeywordArgument) {
                            commandLayout.append("&7${it.key}")
                        } else {
                            commandLayout.append("&8<&7${it.key}&8>")
                        }
                        commandLayout.append(" ")
                    }
                    logger.info(" &8- &7${commandLayout}")
                }
            }
        }
    }
}

fun findSyntaxCommand(command: Command, args: Array<String>): CommandSyntax? {
    for (possibleSyntax in command.commandSyntaxes) {
        val arguments = possibleSyntax.arguments

        if (arguments.size != args.size && arguments.last() !is StringArrayArgument) {
            continue
        }

        val inputContext = InputContext()

        for (i in arguments.indices) {
            val argument = arguments[i]

            if (!argument.predication(args[i])) {
                break
            }

            if (argument is StringArrayArgument) {
                inputContext.append(
                    argument,
                    argument.buildResult(args.sliceArray(i until args.size).joinToString(" "), inputContext)
                )
            } else {
                inputContext.append(argument, argument.buildResult(args[i], inputContext))
            }

            if (arguments.last() == argument) {
                possibleSyntax.execution.execute(inputContext)
                return possibleSyntax
            }
        }
    }
    return null
}