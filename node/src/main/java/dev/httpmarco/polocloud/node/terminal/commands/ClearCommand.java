package dev.httpmarco.polocloud.node.terminal.commands;

import dev.httpmarco.polocloud.node.commands.Command;
import dev.httpmarco.polocloud.node.terminal.JLineTerminal;

public final class ClearCommand extends Command {

    public ClearCommand(JLineTerminal terminal) {
        super("clear", "cc");

        defaultExecution(commandContext -> terminal.clear());
    }
}
