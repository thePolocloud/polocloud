package de.polocloud.cli.command

import de.polocloud.cli.command.arguments.InputContext
import de.polocloud.cli.command.arguments.type.StringArrayArgument
import de.polocloud.cli.logger
import de.polocloud.i18n.api.TranslationService

/**
 * Parses raw terminal input and dispatches it to the correct [Command] and [CommandSyntax].
 *
 * On each [parse] call, the parser:
 * 1. Looks up the command by name via [CommandService].
 * 2. If no arguments are given, invokes the [Command.defaultExecution] or prints help.
 * 3. Otherwise, iterates over all registered syntaxes and finds the first match via argument predication.
 * 4. Builds an [InputContext] with the parsed values and invokes the matched [CommandSyntax.execution].
 *
 * @param commandService The [CommandService] used to resolve commands by name.
 */
class CommandParser(private val commandService: CommandService) {

    /**
     * Parses and dispatches the command identified by [commandId] with the given [args].
     *
     * Prints help output if the command is unknown, no-arg default is missing, or no syntax matches.
     *
     * @param commandId The command name or alias entered by the user.
     * @param args The arguments that followed the command name.
     */
    fun parse(commandId: String, args: Array<String>) {
        val matches = commandService.findByName(commandId)

        if (matches.isEmpty()) {
            printUnknownCommand(commandId)
            return
        }

        val command = matches.first()

        if (args.isEmpty()) {
            command.defaultExecution?.execute(InputContext()) ?: printHelp(command)
            return
        }

        if (matchSyntax(command, args) == null) {
            printHelp(command)
        }
    }

    /**
     * Attempts to find and execute a [CommandSyntax] that matches the given [args].
     *
     * Each syntax is tried in registration order. A syntax matches when every argument passes
     * its [dev.httpmarco.polocloud.cli.command.arguments.TerminalArgument.predication] check. [StringArrayArgument] consumes all remaining
     * tokens as a single joined string.
     *
     * @param command The command whose syntaxes are evaluated.
     * @param args The user-provided argument tokens.
     * @return The matched [CommandSyntax] if one was found and executed, or `null` otherwise.
     */
    private fun matchSyntax(command: Command, args: Array<String>): CommandSyntax? {
        var wrongReason: String? = null

        for (syntax in command.syntaxes) {
            val arguments = syntax.arguments
            val lastArgument = arguments.lastOrNull() ?: continue

            // Argument count must match unless the last argument consumes remaining tokens
            if (arguments.size != args.size && lastArgument !is StringArrayArgument) {
                continue
            }

            val context = InputContext()
            var matched = false

            for (i in arguments.indices) {
                val argument = arguments[i]
                val rawInput = args.getOrNull(i) ?: break

                if (!argument.predication(rawInput)) {
                    if (wrongReason == null) {
                        val reason = argument.wrongReason(rawInput)
                        if (reason.isNotEmpty()) {
                            wrongReason = reason
                        }
                    }
                    break
                }

                val value = if (argument is StringArrayArgument) {
                    argument.buildResult(args.drop(i).joinToString(" "), context)
                } else {
                    argument.buildResult(rawInput, context)
                }

                context.append(argument, value)

                if (argument == lastArgument) {
                    syntax.execution.execute(context)
                    matched = true
                    break
                }
            }

            if (matched) return syntax
        }

        if (wrongReason != null) {
            logger.info(wrongReason)
        }

        return null
    }

    /**
     * Logs all registered syntaxes for [command] as help output.
     *
     * @param command The command whose usage lines should be printed.
     */
    private fun printHelp(command: Command) {
        command.syntaxes.forEach { syntax ->
            logger.info(" &8- &7${syntax.usage()}")
        }
    }

    /**
     * Logs an "unknown command" message for [commandId], along with a "did you mean" hint
     * if a registered command name is reasonably close (small edit distance).
     *
     * @param commandId The unrecognized command name entered by the user.
     */
    private fun printUnknownCommand(commandId: String) {
        logger.info(TranslationService.tr("cli", "cli.command.unknown", "command" to commandId))

        val suggestion = commandService.registeredCommands()
            .map { it.name }
            .minByOrNull { levenshtein(commandId.lowercase(), it.lowercase()) }
            ?.takeIf { levenshtein(commandId.lowercase(), it.lowercase()) <= SUGGESTION_MAX_DISTANCE }

        if (suggestion != null) {
            logger.info(TranslationService.tr("cli", "cli.command.unknown.suggestion", "suggestion" to suggestion))
        }
    }

    /**
     * Computes the Levenshtein (edit) distance between [a] and [b].
     *
     * Used only for a cheap "did you mean" hint on unknown commands - not a full
     * fuzzy-matching implementation.
     */
    private fun levenshtein(a: String, b: String): Int {
        val costs = IntArray(b.length + 1) { it }

        for (i in 1..a.length) {
            var previous = costs[0]
            costs[0] = i

            for (j in 1..b.length) {
                val current = costs[j]
                costs[j] = if (a[i - 1] == b[j - 1]) {
                    previous
                } else {
                    1 + minOf(previous, costs[j], costs[j - 1])
                }
                previous = current
            }
        }

        return costs[b.length]
    }

    private companion object {
        private const val SUGGESTION_MAX_DISTANCE = 2
    }
}