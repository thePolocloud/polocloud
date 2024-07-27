package dev.httpmarco.polocloud.node.commands.type;

import dev.httpmarco.polocloud.node.commands.CommandArgument;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public final class IntArgument extends CommandArgument<Integer> {

    public IntArgument(String key) {
        super(key);
    }

    @Override
    public boolean predication(@NotNull String rawInput) {
        try {
            Integer.parseInt(rawInput);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Contract(pure = true)
    @Override
    public @NotNull String wrongReason() {
        return "The argument " + key() + " is not a number!";
    }

    @Contract(pure = true)
    @Override
    public @Unmodifiable List<String> defaultArgs() {
        return List.of();
    }

    @Contract(pure = true)
    @Override
    public @NotNull Integer buildResult(String input) {
        return Integer.parseInt(input);
    }
}
