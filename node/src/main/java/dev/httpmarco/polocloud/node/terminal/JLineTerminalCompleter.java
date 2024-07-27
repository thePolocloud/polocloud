package dev.httpmarco.polocloud.node.terminal;

import dev.httpmarco.polocloud.node.commands.CommandService;
import dev.httpmarco.polocloud.node.commands.SyntaxCommand;
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

    private CommandService commandService;

    @Override
    public void complete(LineReader lineReader, @NotNull ParsedLine parsedLine, List<Candidate> list) {
        if (parsedLine.wordIndex() == 0) {
            // we only display the command names -> not aliases

            for (var command : commandService.commands()) {
                // if one command start with the given command first word
                if (command.name().startsWith(parsedLine.word())) {
                    list.add(new Candidate(command.name()));
                }

                Arrays.stream(command.aliases()).filter(it -> it.startsWith(parsedLine.word())).forEach(s -> list.add(new Candidate(s)));
            }
            return;
        }

        var commandName = parsedLine.words().get(0);

        for (var command : commandService.commandsByName(commandName)) {

            for (SyntaxCommand syntaxCommand : command.syntaxCommands) {

                if (syntaxCommand.arguments().length < parsedLine.wordIndex()) {
                    continue;
                }


                for (int i = 0; i < syntaxCommand.arguments().length; i++) {
                    if ((i + 1) != parsedLine.wordIndex()) {
                        continue;
                    }

                    var argument = syntaxCommand.arguments()[i];

                    if (argument.defaultArgs().isEmpty()) {
                        list.add(new Candidate("<" + argument.key() + ">"));
                        continue;
                    }

                    list.addAll(argument.defaultArgs().stream().map(Candidate::new).toList());
                }
            }
        }
    }
}