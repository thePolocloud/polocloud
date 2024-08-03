package dev.httpmarco.polocloud.node.platforms.patcher;

import dev.httpmarco.polocloud.node.platforms.PlatformPatcher;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public abstract class AbstractPlatformPatcher implements PlatformPatcher {

    private final String patchId;

}
