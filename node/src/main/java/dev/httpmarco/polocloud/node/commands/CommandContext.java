package dev.httpmarco.polocloud.node.commands;

import org.jetbrains.annotations.Contract;

import java.util.HashMap;
import java.util.Map;

public final class CommandContext {

    private final Map<CommandArgument<?>, Object> contexts = new HashMap<>();

    @Contract(pure = true)
    @SuppressWarnings("unchecked")
    public <T> T arg(CommandArgument<T> argument) {
        return (T) contexts.get(argument);
    }

    public <T> void append(CommandArgument<?> argument, Object value) {
        this.contexts.put(argument, value);
    }
}
