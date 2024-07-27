package dev.httpmarco.polocloud.node.groups;

import dev.httpmarco.polocloud.api.groups.ClusterGroup;
import dev.httpmarco.polocloud.api.platforms.PlatformGroupDisplay;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class ClusterGroupImpl implements ClusterGroup {

    private final String name;
    private final PlatformGroupDisplay platform;

    private String[] node;
    private int minMemory;
    private int maxMemory;
    private boolean staticService;
    private int minOnlineServerInstances;
    private int maxOnlineServerInstances;

}
