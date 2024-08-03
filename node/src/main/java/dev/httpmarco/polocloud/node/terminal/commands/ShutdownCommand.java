package dev.httpmarco.polocloud.node.terminal.commands;

import dev.httpmarco.polocloud.node.NodeShutdown;
import dev.httpmarco.polocloud.node.commands.Command;

public final class ShutdownCommand extends Command {

    public ShutdownCommand() {
        super("shutdown", "stop", "exit");
        defaultExecution(commandContext -> NodeShutdown.nodeShutdown());
    }
}
