package dev.httpmarco.polocloud.node.logging;

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;

@Plugin(name = "Log4j2Appender", category = Core.CATEGORY_NAME, elementType = "appender", printObject = true)
public final class Log4j2Appender extends AbstractAppender {

    private static final SimpleDateFormat TERMINAL_LAYOUT = new SimpleDateFormat("HH:mm:ss");

    public Log4j2Appender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }

    @Contract(value = " -> new", pure = true)
    @PluginBuilderFactory
    public static @NotNull Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AbstractAppender.Builder<Builder> implements org.apache.logging.log4j.core.util.Builder<Log4j2Appender> {

        @Override
        public Log4j2Appender build() {
            return new Log4j2Appender(getName(), getFilter(), getLayout(), isIgnoreExceptions(), getPropertyArray());
        }
    }

    @Override
    public void append(@NotNull LogEvent event) {
        var message = event.getMessage().getFormattedMessage();
        var layout = "&7" + TERMINAL_LAYOUT.format(Calendar.getInstance().getTime()) + " &8| " + Log4jColorTranslate.translate(event.getLevel()) + event.getLevel().name() + "&8: &7" + message;
        System.out.println(layout);
    }
}
