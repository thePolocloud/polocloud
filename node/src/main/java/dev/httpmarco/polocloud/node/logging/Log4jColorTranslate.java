package dev.httpmarco.polocloud.node.logging;

import dev.httpmarco.polocloud.node.terminal.JLineTerminalColor;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.Level;

@UtilityClass
public class Log4jColorTranslate {

    public String translate(Level level) {
        if (level == Level.WARN) {
            return JLineTerminalColor.YELLOW.code();
        }
        if (level == Level.INFO) {
            return JLineTerminalColor.CYAN.code();
        }
        if (level == Level.ERROR) {
            return JLineTerminalColor.RED.code();
        }
        if (level == Level.DEBUG) {
            return JLineTerminalColor.WHITE.code();
        }
        if (level == Level.FATAL) {
            return JLineTerminalColor.RED.code();
        }
        return JLineTerminalColor.GRAY.code();
    }
}
