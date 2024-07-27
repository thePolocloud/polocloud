package dev.httpmarco.polocloud.node.cluster;

import dev.httpmarco.polocloud.api.Closeable;

import java.util.Set;

public interface ClusterService extends Closeable {

    LocalNode localNode();

    NodeEndpoint headNode();

    Set<NodeEndpoint> endpoints();

    void initialize();

}
