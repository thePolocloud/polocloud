package dev.httpmarco.polocloud.node.terminal;

import dev.httpmarco.polocloud.node.Node;
import dev.httpmarco.polocloud.node.NodeConfig;
import dev.httpmarco.polocloud.node.NodeShutdown;
import dev.httpmarco.polocloud.node.terminal.util.TerminalColorUtil;
import lombok.extern.slf4j.Slf4j;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;

import java.util.Arrays;

@Slf4j
public final class JLineCommandReadingThread extends Thread {

    private final NodeConfig localNodeImpl;
    private final JLineTerminal terminal;


    public JLineCommandReadingThread(NodeConfig localNodeImpl, JLineTerminal terminal) {
        this.localNodeImpl = localNodeImpl;
        this.terminal = terminal;

        setContextClassLoader(ClassLoader.getSystemClassLoader());
    }

    @Override
    public void run() {

        var prompt = TerminalColorUtil.replaceColorCodes("&9" + localNodeImpl.localNode().name() + "&8@&7cloud &8» &7");

        while (!isInterrupted()) {
            try {
                try {
                    try {
                        var rawLine = terminal.lineReader().readLine(prompt);

                        if (rawLine.isEmpty()) {
                            continue;
                        }

                        final var line = rawLine.split(" ");

                        if (line.length > 0) {
                            Node.instance().commandService().call(line[0], Arrays.copyOfRange(line, 1, line.length));
                        }
                    } catch (EndOfFileException ignore) {
                    }
                } catch (UserInterruptException exception) {
                    NodeShutdown.nodeShutdown();
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }
}
