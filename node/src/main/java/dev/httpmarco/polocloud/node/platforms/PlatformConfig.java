package dev.httpmarco.polocloud.node.platforms;

import dev.httpmarco.polocloud.node.platforms.Platform;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public final class PlatformConfig {

    private int version;
    private Platform[] platforms;

}
