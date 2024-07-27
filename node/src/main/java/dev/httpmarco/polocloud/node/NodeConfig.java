package dev.httpmarco.polocloud.node;

import dev.httpmarco.polocloud.node.cluster.NodeEndpointData;
import dev.httpmarco.polocloud.node.util.StringUtils;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.Set;

@Getter
@Accessors(fluent = true)
public final class NodeConfig {

    private final String clusterId;
    private final String clusterToken;
    private final NodeEndpointData localNode;
    private final Set<NodeEndpointData> nodes;

    public NodeConfig() {
        this.clusterId = "polocloud-cluster";
        this.clusterToken = StringUtils.randomString(8);
        this.localNode = new NodeEndpointData("node-" + StringUtils.randomString(4), "127.0.0.1", 9090);
        this.nodes = new HashSet<>();
    }
}
