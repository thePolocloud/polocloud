package dev.httpmarco.polocloud.node.terminal.commands;

import dev.httpmarco.polocloud.node.Node;
import dev.httpmarco.polocloud.node.commands.Command;

public final class ClearCommand extends Command {

    public ClearCommand() {
        super("clear", "cc");
        defaultExecution(commandContext -> Node.instance().terminal().clear());
    }
}
