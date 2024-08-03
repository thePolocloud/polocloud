package dev.httpmarco.polocloud.node.terminal;

import dev.httpmarco.polocloud.api.Closeable;
import dev.httpmarco.polocloud.node.NodeConfig;
import dev.httpmarco.polocloud.node.logging.Log4j2Stream;
import dev.httpmarco.polocloud.node.terminal.util.TerminalColorUtil;
import dev.httpmarco.polocloud.node.terminal.util.TerminalHeader;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.jline.jansi.Ansi;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import java.nio.charset.StandardCharsets;

@Getter
@Accessors(fluent = true)
@Log4j2
public final class JLineTerminal implements Closeable {

    private final Terminal terminal;
    private final LineReader lineReader;
    private final JLineCommandReadingThread commandReadingThread;

    @SneakyThrows
    public JLineTerminal(NodeConfig config) {
        this.terminal = TerminalBuilder.builder()
                .system(true)
                .encoding(StandardCharsets.UTF_8)
                .dumb(true)
                .jansi(true)
                .build();

        this.lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new JLineTerminalCompleter())

                .option(LineReader.Option.AUTO_MENU_LIST, true)
                // change color of selection box
                .variable(LineReader.COMPLETION_STYLE_LIST_SELECTION, "fg:cyan")
                .variable(LineReader.COMPLETION_STYLE_LIST_BACKGROUND, "bg:default")

                .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                .option(LineReader.Option.AUTO_PARAM_SLASH, false)
                .variable(LineReader.BELL_STYLE, "none")
                .build();


        System.setOut(new Log4j2Stream(this::printLine).printStream());
        System.setErr(new Log4j2Stream(log::error).printStream());

        this.commandReadingThread = new JLineCommandReadingThread(config, this);

        clear();
        TerminalHeader.print(this, config);
    }

    public void allowInput() {
        commandReadingThread().start();
    }

    public void clear() {
        this.terminal.puts(InfoCmp.Capability.clear_screen);
        this.terminal.flush();
        this.update();
    }

    public void update() {
        if (this.lineReader.isReading()) {
            this.lineReader.callWidget(LineReader.REDRAW_LINE);
            this.lineReader.callWidget(LineReader.REDISPLAY);
        }
    }

    public void printLine(String message) {
        terminal.puts(InfoCmp.Capability.carriage_return);
        terminal.writer().println(TerminalColorUtil.replaceColorCodes(message) + Ansi.ansi().a(Ansi.Attribute.RESET).toString());
        terminal.flush();
        this.update();
    }

    @Override
    @SneakyThrows
    public void close() {
        this.commandReadingThread.interrupt();
        this.terminal.close();
    }
}
