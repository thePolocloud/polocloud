package dev.httpmarco.polocloud.node.platforms;

import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * paper
 * velocity
 * purpur
 * spigot
 * bungeecord
 * sponge powered
 * minestom
 * multipaper
 * fabric
 * (folia)
 */

@Setter
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public final class Platform {

    private final String platform;
    private final PlatformType type;
    private final Set<PlatformVersion> versions;

    private @Nullable PlatformPatcher platformPatcher;
    private @Nullable String[] startArguments;

}
