package dev.httpmarco.polocloud.node.groups;

import dev.httpmarco.polocloud.api.groups.ClusterGroup;
import dev.httpmarco.polocloud.api.platforms.PlatformGroupDisplay;
import dev.httpmarco.polocloud.api.services.ClusterService;
import dev.httpmarco.polocloud.node.Node;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.List;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class ClusterGroupImpl implements ClusterGroup {

    private final String name;
    private final PlatformGroupDisplay platform;

    private String[] nodes;
    private int minMemory;
    private int maxMemory;
    private boolean staticService;
    private int minOnlineServerInstances;
    private int maxOnlineServerInstances;

    @Override
    public long serviceCount() {
        return Node.instance().serviceProvider().services().stream().filter(it -> it.group().equals(this)).count();
    }

    @Override
    public List<ClusterService> services() {
        return Node.instance().serviceProvider().services().stream().filter(it -> it.group().equals(this)).toList();
    }

    @Override
    public String details() {
        return "platform&8=&7" + platform.details() + "&8, &7nodes&8=&7" + Arrays.toString(nodes) + ", &7minMemory&8=&7" + minMemory + "&8, &7maxMemory&8=&7" + maxMemory + "&8, &7static&8=&7" + staticService;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClusterGroup clusterGroup && clusterGroup.name().equals(name);
    }
}
