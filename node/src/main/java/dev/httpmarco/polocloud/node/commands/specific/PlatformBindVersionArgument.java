package dev.httpmarco.polocloud.node.commands.specific;

import dev.httpmarco.polocloud.node.commands.CommandArgument;
import dev.httpmarco.polocloud.node.commands.CommandContext;
import dev.httpmarco.polocloud.node.platforms.PlatformVersion;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class PlatformBindVersionArgument extends CommandArgument<PlatformVersion> {

    public PlatformBindVersionArgument(String key) {
        super(key);
    }

    @Contract(pure = true)
    @Override
    public @NotNull PlatformVersion buildResult(String input) {
        return new PlatformVersion(input, null, null);
    }

    @Override
    public boolean predication(@NotNull String rawInput) {
        return super.predication(rawInput);
    }

    @Override
    public List<String> defaultArgs(@NotNull CommandContext previousBind) {
        return previousBind.arg(PlatformArgument.class).versions().stream().map(PlatformVersion::version).toList();
    }
}
