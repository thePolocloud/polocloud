package dev.httpmarco.polocloud.node.commands;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public final class CommandMultiBindingArgument extends CommandArgument<Object> {

    private CommandArgument<?>[] arguments;

    public CommandMultiBindingArgument(String key) {
        super(key);
    }

    @Contract(pure = true)
    @Override
    public @Nullable Object buildResult(String input) {
        return null;
    }
}
