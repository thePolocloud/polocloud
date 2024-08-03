package dev.httpmarco.polocloud.api.platforms;

import dev.httpmarco.polocloud.api.Detail;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record PlatformGroupDisplay(String platform, String version) implements Detail {

    @Contract(pure = true)
    @Override
    public @NotNull String details() {
        return platform + "-" + version;
    }
}
