package dev.httpmarco.polocloud.base.terminal;

import dev.httpmarco.polocloud.base.CloudBase;
import org.fusesource.jansi.Ansi;
import org.jline.reader.UserInterruptException;

import java.util.logging.Level;

public final class CloudTerminalThread extends Thread {

    private final String prompt;
    private final CloudTerminal terminal;

    public CloudTerminalThread(CloudTerminal terminal) {
        this.terminal = terminal;

        setName("console-reading-thread");
        this.prompt = this.terminal.includeColorCodes(CloudBase.instance().configuration().promt()
                .replace("%username%", System.getProperty("user.name")));

        setContextClassLoader(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                final var line = terminal.lineReader().readLine(prompt).split(" ");
                this.terminal.print(Level.OFF, Ansi.ansi().reset().cursorUp(1).eraseLine().toString(), null);

                if (line.length > 0) {
                    terminal.commandService().call(line);
                }
            }
        } catch (UserInterruptException exception) {
            System.exit(-1);
        }
    }
}
