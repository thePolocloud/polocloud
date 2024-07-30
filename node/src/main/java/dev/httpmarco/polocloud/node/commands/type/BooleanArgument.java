package dev.httpmarco.polocloud.node.commands.type;

import dev.httpmarco.polocloud.node.commands.CommandArgument;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public final class BooleanArgument extends CommandArgument<Boolean> {

    public BooleanArgument(String key) {
        super(key);
    }

    @Override
    public boolean predication(@NotNull String rawInput) {
        return rawInput.equalsIgnoreCase("true") || rawInput.equalsIgnoreCase("false");
    }

    @Contract(pure = true)
    @Override
    public @NotNull String wrongReason() {
        return "The argument " + key() + " is not a boolean!";
    }

    @Contract(pure = true)
    @Override
    public @Unmodifiable List<String> defaultArgs() {
        return List.of("false", "true");
    }

    @Contract(pure = true)
    @Override
    public @NotNull Boolean buildResult(String input) {
        return Boolean.parseBoolean(input);
    }
}
