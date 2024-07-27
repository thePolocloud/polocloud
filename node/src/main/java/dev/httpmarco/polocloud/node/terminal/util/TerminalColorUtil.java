package dev.httpmarco.polocloud.node.terminal.util;

import dev.httpmarco.polocloud.node.terminal.JLineTerminalColor;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TerminalColorUtil {

    private static final String COLOR_INDEX = "&";

    public String replaceColorCodes(String message) {
        for (var color : JLineTerminalColor.values()) {
            message = message.replace((COLOR_INDEX + color.key()), color.ansiCode());
        }
        return message;
    }
}
