package dev.httpmarco.polocloud.node.terminal;

import dev.httpmarco.polocloud.node.NodeConfig;
import dev.httpmarco.polocloud.node.NodeShutdown;
import dev.httpmarco.polocloud.node.cluster.ClusterService;
import dev.httpmarco.polocloud.node.commands.CommandService;
import dev.httpmarco.polocloud.node.terminal.util.TerminalColorUtil;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;

import java.util.Arrays;

public final class JLineCommandReadingThread extends Thread {

    private final NodeConfig localNodeImpl;
    private final ClusterService clusterService;
    private final CommandService commandService;
    private final JLineTerminal terminal;


    public JLineCommandReadingThread(NodeConfig localNodeImpl, ClusterService clusterService, CommandService commandService, JLineTerminal terminal) {
        this.localNodeImpl = localNodeImpl;
        this.commandService = commandService;
        this.terminal = terminal;
        this.clusterService = clusterService;

        setContextClassLoader(ClassLoader.getSystemClassLoader());
    }

    @Override
    public void run() {

        var prompt = TerminalColorUtil.replaceColorCodes("&9" + localNodeImpl.localNode().name() + "&8@&7cloud &8» &7");

        while (!isInterrupted()) {
            try {
                try {
                    var rawLine = terminal.lineReader().readLine(prompt);

                    if (rawLine.isEmpty()) {
                        continue;
                    }

                    final var line = rawLine.split(" ");

                    if (line.length > 0) {
                        commandService.call(line[0], Arrays.copyOfRange(line, 1, line.length));
                    }
                } catch (EndOfFileException ignore) {
                }
            } catch (UserInterruptException exception) {
                NodeShutdown.nodeShutdown(terminal, clusterService);
            }
        }
    }
}
