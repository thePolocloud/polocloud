package dev.httpmarco.polocloud.node.commands.type;

import dev.httpmarco.polocloud.node.commands.CommandArgument;
import dev.httpmarco.polocloud.node.commands.CommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public final  class EnumArgument<E extends Enum<E>> extends CommandArgument<E> {

    private final Class<E> enumClass;

    public EnumArgument(Class<E> enumClass, String key) {
        super(key);

        this.enumClass = enumClass;
    }

    @Override
    public List<String> defaultArgs(CommandContext context) {
        return Arrays.stream(enumClass.getEnumConstants()).map(e -> e.name().toLowerCase()).toList();
    }

    @Override
    public boolean predication(@NotNull String rawInput) {
        return Arrays.stream(enumClass.getEnumConstants()).anyMatch(it -> rawInput.equalsIgnoreCase(it.name().toLowerCase()));
    }

    @Override
    public @NotNull E buildResult(String input) {
        return Enum.valueOf(enumClass, input.toUpperCase());
    }
}
