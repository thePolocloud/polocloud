package dev.httpmarco.polocloud.node.commands.type;

import dev.httpmarco.polocloud.node.commands.CommandArgument;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class TextArgument extends CommandArgument<String> {

    public TextArgument(String key) {
        super(key);
    }

    @Contract(pure = true)
    @Override
    public @NotNull String buildResult(String input) {
        return input;
    }
}
