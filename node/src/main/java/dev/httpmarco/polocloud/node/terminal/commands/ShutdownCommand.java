package dev.httpmarco.polocloud.node.terminal.commands;

import dev.httpmarco.polocloud.node.NodeShutdown;
import dev.httpmarco.polocloud.node.cluster.ClusterService;
import dev.httpmarco.polocloud.node.commands.Command;
import dev.httpmarco.polocloud.node.terminal.JLineTerminal;

public final class ShutdownCommand extends Command {

    public ShutdownCommand(JLineTerminal terminal, ClusterService cluster) {
        super("shutdown", "stop", "exit");
        defaultExecution(commandContext -> NodeShutdown.nodeShutdown(terminal, cluster));
    }
}
