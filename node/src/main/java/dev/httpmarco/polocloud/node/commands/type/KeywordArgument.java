package dev.httpmarco.polocloud.node.commands.type;

import dev.httpmarco.polocloud.node.commands.CommandArgument;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public final class KeywordArgument extends CommandArgument<String> {

    public KeywordArgument(String key) {
        super(key);
    }

    @Contract(" -> new")
    @Override
    public @NotNull @Unmodifiable List<String> defaultArgs() {
        return List.of(key());
    }

    @Contract(pure = true)
    @Override
    public @NotNull String wrongReason() {
        return "";
    }

    @Contract(pure = true)
    @Override
    public @NotNull String buildResult(String input) {
        return "";
    }
}
