package dev.httpmarco.polocloud.node.platforms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * paper
 * velocity
 * vanilla
 * purpur
 * spigot
 * bungeecord
 * sponge powered
 * minestom
 * multipaper
 * fabric
 * (folia)
 */

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public final class Platform {

    private final String platform;
    private final PlatformType type;
    private final Set<PlatformVersion> versions;

    @Setter
    private @Nullable PlatformPatcher platformPatcher;

}
