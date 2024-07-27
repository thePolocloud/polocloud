package dev.httpmarco.polocloud.node.commands.type;

import dev.httpmarco.polocloud.node.commands.CommandArgument;
import dev.httpmarco.polocloud.node.platforms.Platform;
import dev.httpmarco.polocloud.node.platforms.PlatformService;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public final class PlatformArgument extends CommandArgument<Platform> {

    private final PlatformService platformService;

    public PlatformArgument(String key, PlatformService platformService) {
        super(key);
        this.platformService = platformService;
    }

    @Override
    public boolean predication(@NotNull String rawInput) {
        return this.platformService.exists(rawInput);
    }

    @Override
    public List<String> defaultArgs() {
        return Arrays.stream(platformService.platforms()).map(Platform::platform).toList();
    }

    @Contract(pure = true)
    @Override
    public @Nullable Platform buildResult(String input) {
        return platformService.platform(input);
    }
}
