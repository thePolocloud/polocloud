package dev.httpmarco.polocloud.base.terminal;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.fusesource.jansi.Ansi;

@Getter
public enum CloudTerminalColor {

    DEFAULT(209, 209, 209),
    DARK_GRAY(69, 69, 69),

    WHITE(255, 255, 255),

    INFO(125, 246, 255),
    WARNING(176, 130, 23),
    ERROR(161, 46, 40),
    PROMPT(130, 234, 255);

    public static final CloudTerminalColor[] colors = values();

    @Accessors(fluent = true)
    private final String ansiCode;

    CloudTerminalColor(int red, int green, int blue) {
        this.ansiCode = Ansi.ansi().a(Ansi.Attribute.RESET).fgRgb(red, green, blue).toString();
    }
}
