package dev.httpmarco.polocloud.base.logging.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

import java.util.HashMap;
import java.util.Map;

public class ColorConverter extends CompositeConverter<ILoggingEvent> {

    private static final Map<Integer, AnsiColor> LEVEL_COLORS;

    static {
        LEVEL_COLORS = new HashMap<>();
        LEVEL_COLORS.put(Level.ERROR_INT, AnsiColor.RED);
        LEVEL_COLORS.put(Level.WARN_INT, AnsiColor.YELLOW);
        LEVEL_COLORS.put(Level.INFO_INT, AnsiColor.GREEN);
        LEVEL_COLORS.put(Level.DEBUG_INT, AnsiColor.BLUE);
        LEVEL_COLORS.put(Level.TRACE_INT, AnsiColor.BLACK);
    }

    @Override
    protected String transform(ILoggingEvent iLoggingEvent, String input) {
        var colorName = getFirstOption();
        var ansiColor = AnsiColor.fromName(colorName);
        if (ansiColor != null) {
            return ansiColor.colorize(input);
        }
        var level = iLoggingEvent.getLevel().toInt();
        var levelColor = LEVEL_COLORS.getOrDefault(level, AnsiColor.WHITE);
        return levelColor.colorize(input);
    }
}