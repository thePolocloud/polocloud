package de.polocloud.cli.terminal

import de.polocloud.cli.command.CommandService
import de.polocloud.cli.command.arguments.InputContext
import de.polocloud.cli.command.arguments.TerminalArgument
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

/**
 * Provides tab-completion suggestions for the CLI terminal input.
 *
 * Registered as the completer in [CliTerminal] via JLine's [org.jline.reader.LineReaderBuilder].
 *
 * Completion walks the already-typed words: while the first word is being typed, all registered
 * command names are suggested. Once a known command name is present, the words preceding the one
 * currently being completed are parsed into an [InputContext] so that context-aware arguments can
 * offer the correct, dependent suggestions, and the argument at the current position is asked for
 * its [TerminalArgument.defaultArgs].
 *
 * @param commandService The [CommandService] used to resolve commands and their syntaxes.
 */
class TabCompleter(private val commandService: CommandService) : Completer {

    override fun complete(
        reader: LineReader,
        line: ParsedLine,
        candidates: MutableList<Candidate>
    ) {
        val words = line.words()
        val wordIndex = line.wordIndex()

        // Still typing the command name itself.
        if (wordIndex == 0) {
            commandService.registeredCommands().forEach { candidates.add(Candidate(it.name)) }
            return
        }

        val command = commandService.findByName(words[0]).firstOrNull() ?: return
        val argIndex = wordIndex - 1

        for (syntax in command.syntaxes) {
            val arguments = syntax.arguments
            if (argIndex >= arguments.size) continue

            val context = InputContext()
            if (!fillContext(arguments, words, argIndex, context)) continue

            arguments[argIndex].defaultArgs(context).forEach { candidates.add(Candidate(it)) }
        }
    }

    /**
     * Parses the words preceding the one being completed into [context].
     *
     * Returns `false` as soon as a word does not satisfy its argument, marking the syntax
     * as not applicable for the current input.
     */
    private fun fillContext(
        arguments: List<TerminalArgument<*>>,
        words: List<String>,
        argIndex: Int,
        context: InputContext
    ): Boolean {
        for (i in 0 until argIndex) {
            val argument = arguments[i]
            val raw = words.getOrNull(i + 1) ?: return false

            if (!argument.predication(raw)) return false

            val appended = runCatching { context.append(argument, argument.buildResult(raw, context)) }.isSuccess
            if (!appended) return false
        }
        return true
    }
}
