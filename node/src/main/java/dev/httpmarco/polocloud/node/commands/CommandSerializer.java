package dev.httpmarco.polocloud.node.commands;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Log4j2
@UtilityClass
public class CommandSerializer {

    public void serializer(@NotNull CommandService commandService, String name, String[] args) {
        // all command with same start name
        List<Command> commands = commandService.commandsByName(name);


        if (executeCommand(commands, args)) {
            return;
        }

        // we must calculate the usage, because no command was found

        for (var command : commands) {
            for (var syntaxCommand : command.syntaxCommands) {
                log.info("{} {}", command.name(), syntaxCommand.usage());
            }
        }
    }

    private boolean executeCommand(@NotNull List<Command> commands, String[] args) {
        for (var command : commands) {

            if (command.syntaxCommands.isEmpty()) {
                if (command.defaultExecution() != null) {
                    command.defaultExecution().execute(new CommandContext());
                }
                return true;
            }

            for (var syntaxCommand : command.syntaxCommands) {

                if (args.length != syntaxCommand.arguments().length) {
                    continue;
                }

                var commandContext = new CommandContext();

                for (int i = 0; i < syntaxCommand.arguments().length; i++) {
                    var argument = syntaxCommand.arguments()[i];
                    var rawInput = args[i];

                    if (!argument.predication(rawInput)) {
                        log.warn(argument.wrongReason());
                        return true;
                    }

                    commandContext.append(argument, argument.buildResult(rawInput));
                }

                syntaxCommand.execution().execute(commandContext);
                return true;
            }
        }
        return false;
    }
}