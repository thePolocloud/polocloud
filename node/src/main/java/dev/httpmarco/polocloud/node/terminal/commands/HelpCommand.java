package dev.httpmarco.polocloud.node.terminal.commands;

import dev.httpmarco.polocloud.node.Node;
import dev.httpmarco.polocloud.node.commands.Command;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class HelpCommand extends Command {

    public HelpCommand() {
        super("help", "Help you with all commands", "?");

        defaultExecution(context -> {
            for (var command : Node.instance().commandService().commands()) {
                var aliases = command.aliases().length != 0 ? " &8(&7" + String.join("&8, &7", command.aliases()) + "&8)" : "";

                log.info("&f{}{} &8- &7{}&8.", command.name(), aliases, command.description());
            }
        });
    }
}
