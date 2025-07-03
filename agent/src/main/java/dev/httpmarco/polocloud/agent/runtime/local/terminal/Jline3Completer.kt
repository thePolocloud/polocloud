package dev.httpmarco.polocloud.agent.runtime.local.terminal

import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandContext
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandService
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandSyntax
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine
import java.util.*

class Jline3Completer(private val commandService: CommandService) : Completer {

    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        if (line.wordIndex() == 0) {
            // we only display the command names -> not aliases
            for (command in commandService.commands) {
                // if one command start with the given command first word
                if (command.name.startsWith(line.word())) {
                    candidates.add(Candidate(command.name))
                }

                Arrays.stream(command.aliases)
                    .filter { !line.word().isEmpty() && it.startsWith(line.word()) }
                    .forEach { alias -> candidates.add(Candidate(alias)) }
            }
            return
        }

        val commandName = line.words().first()
        for (command in commandService.commandsByName(commandName!!)) {
            for (commandSyntax in command.commandSyntaxes()) {
                if (isMatchingSyntax(line, commandSyntax)) {
                    addSuggestions(line, commandSyntax, candidates)
                }
            }
        }
    }

    private fun isMatchingSyntax(parsedLine: ParsedLine, commandSyntax: CommandSyntax): Boolean {
        val argumentIndex = parsedLine.wordIndex() - 1
        if (argumentIndex >= commandSyntax.arguments.size) {
            return false
        }
        for (i in 0..<argumentIndex) {
            val expectedArgument = commandSyntax.arguments[i]
            val enteredArgument = parsedLine.words()[i + 1].replace("<", "").replace(">", "")
            if ((expectedArgument.key != enteredArgument && !expectedArgument.predication(enteredArgument))) {
                return false
            }
        }
        return true
    }

    private fun addSuggestions(parsedLine: ParsedLine, commandSyntax: CommandSyntax, list: MutableList<Candidate>) {
        val argumentIndex = parsedLine.wordIndex() - 1
        if (argumentIndex >= commandSyntax.arguments.size) {
            return
        }

        val argument = commandSyntax.arguments[argumentIndex]
        val context = CommandContext()

        for (i in 0..<argumentIndex) {
            // read all previous temp parameters
            val input = parsedLine.words().get(i + 1).replace("<", "").replace(">", "")
            val tempArgument = commandSyntax.arguments[i]
            if (input.equals(tempArgument.key, ignoreCase = true)) {
                continue
            }
            context.append(commandSyntax.arguments[i], commandSyntax.arguments[i].buildResult(input, context))
        }

        if (argument.defaultArgs(context).isEmpty()) {
            val candidateValue = "<" + argument.key + ">"
            if (list.stream().noneMatch { candidate: Candidate? -> candidate!!.value() == candidateValue }) {
                list.add(Candidate(candidateValue))
            }
        } else {
            argument.defaultArgs(context).stream()
                .filter { defaultArg ->
                    list.stream().noneMatch { candidate: Candidate? -> candidate!!.value() == defaultArg }
                }
                .forEach { defaultArg -> list.add(Candidate(defaultArg)) }
        }
    }
}