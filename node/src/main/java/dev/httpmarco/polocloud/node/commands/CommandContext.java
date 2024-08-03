package dev.httpmarco.polocloud.node.commands;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public final class CommandContext {

    private final Map<Class<? extends CommandArgument<?>>, Object> contexts = new HashMap<>();

    @Contract(pure = true)
    @SuppressWarnings("unchecked")
    public <T> T arg(@NotNull CommandArgument<T> argument) {
        return (T) contexts.get(argument.getClass());
    }

    @SuppressWarnings("unchecked")
    public void append(@NotNull CommandArgument<?> argument, Object value) {
        this.contexts.put((Class<? extends CommandArgument<?>>) argument.getClass(), value);
    }

    @SuppressWarnings("unchecked")
    public <T> T arg(Class<? extends CommandArgument<T>> argument) {
        return (T) contexts.get(argument);
    }
}