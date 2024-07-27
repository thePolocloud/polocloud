package dev.httpmarco.polocloud.node.cluster;

import dev.httpmarco.polocloud.api.Closeable;

public interface NodeEndpoint extends Closeable {

    NodeSituation situation();

    NodeEndpointData data();

    void situation(NodeSituation situation);

}
