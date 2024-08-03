package dev.httpmarco.polocloud.node.terminal;

import dev.httpmarco.polocloud.node.Node;
import dev.httpmarco.polocloud.node.commands.CommandContext;
import dev.httpmarco.polocloud.node.commands.CommandSyntax;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.Arrays;
import java.util.List;

@Log4j2
@AllArgsConstructor
public final class JLineTerminalCompleter implements Completer {

    @Override
    public void complete(LineReader lineReader, @NotNull ParsedLine parsedLine, List<Candidate> list) {
        var commandService = Node.instance().commandService();

        if (parsedLine.wordIndex() == 0) {
            // we only display the command names -> not aliases

            for (var command : commandService.commands()) {
                // if one command start with the given command first word
                if (command.name().startsWith(parsedLine.word())) {
                    list.add(new Candidate(command.name()));
                }

                Arrays.stream(command.aliases())
                        .filter(it -> it.startsWith(parsedLine.word()))
                        .forEach(alias -> list.add(new Candidate(alias)));
            }
            return;
        }

        var commandName = parsedLine.words().get(0);
        for (var command : commandService.commandsByName(commandName)) {

            for (CommandSyntax commandSyntax : command.commandSyntaxes()) {

                if (isMatchingSyntax(parsedLine, commandSyntax)) {
                    addSuggestions(parsedLine, commandSyntax, list);
                }
            }
        }
    }

    private boolean isMatchingSyntax(@NotNull ParsedLine parsedLine, @NotNull CommandSyntax commandSyntax) {
        var argumentIndex = parsedLine.wordIndex() - 1;
        if (argumentIndex >= commandSyntax.arguments().length) {
            return false;
        }

        for (int i = 0; i < argumentIndex; i++) {
            var expectedArgument = commandSyntax.arguments()[i];
            var enteredArgument = parsedLine.words().get(i + 1).replace("<", "").replace(">", "");

            if ((!expectedArgument.key().equals(enteredArgument) && !expectedArgument.predication(enteredArgument))) {
                return false;
            }
        }
        return true;
    }

    private void addSuggestions(@NotNull ParsedLine parsedLine, @NotNull CommandSyntax commandSyntax, List<Candidate> list) {
        var argumentIndex = parsedLine.wordIndex() - 1;
        if (argumentIndex >= commandSyntax.arguments().length) {
            return;
        }

        var argument = commandSyntax.arguments()[argumentIndex];
        var context = new CommandContext();

        for (int i = 0; i < argumentIndex; i++) {
            // read all previous temp parameters
            var input = parsedLine.words().get(i + 1).replace("<", "").replace(">", "");
            var tempArgument = commandSyntax.arguments()[i];
            if (input.equalsIgnoreCase(tempArgument.key())) {
                continue;
            }
            context.append(commandSyntax.arguments()[i], commandSyntax.arguments()[i].buildResult(input));
        }

        if (argument.defaultArgs(context).isEmpty()) {
            String candidateValue = "<" + argument.key() + ">";
            if (list.stream().noneMatch(candidate -> candidate.value().equals(candidateValue))) {
                list.add(new Candidate(candidateValue));
            }
        } else {
            argument.defaultArgs(context).stream()
                    .filter(defaultArg -> list.stream().noneMatch(candidate -> candidate.value().equals(defaultArg)))
                    .forEach(defaultArg -> list.add(new Candidate(defaultArg)));
        }
    }
}