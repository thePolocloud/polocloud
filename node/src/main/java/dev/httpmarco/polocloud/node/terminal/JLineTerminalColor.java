package dev.httpmarco.polocloud.node.terminal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jline.jansi.Ansi;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public enum JLineTerminalColor {

    GRAY('7', Ansi.ansi().reset().fg(Ansi.Color.WHITE)),
    DARK_GRAY('8', Ansi.ansi().reset().fg(Ansi.Color.BLACK).bold()),
    BLUE('9', Ansi.ansi().reset().fg(Ansi.Color.CYAN)),
    CYAN('b', Ansi.ansi().reset().fg(Ansi.Color.CYAN).bold()),
    YELLOW('e', Ansi.ansi().reset().fg(Ansi.Color.YELLOW)),
    RED('c', Ansi.ansi().reset().fg(Ansi.Color.RED).bold()),
    WHITE('f', Ansi.ansi().reset().fg(Ansi.Color.WHITE).bold());

    private final char key;
    private final String ansiCode;

    JLineTerminalColor(char key, @NotNull Ansi ansiCode) {
        this.key = key;
        this.ansiCode = ansiCode.toString();
    }

    @Contract(pure = true)
    public @NotNull String code() {
        return "&" + key;
    }
}
