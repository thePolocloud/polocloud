package dev.httpmarco.polocloud.node.cluster.impl;

import com.google.inject.Singleton;
import dev.httpmarco.osgan.networking.server.CommunicationServer;
import dev.httpmarco.polocloud.node.cluster.LocalNode;
import dev.httpmarco.polocloud.node.cluster.NodeEndpointData;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Singleton
public final class LocalNodeImpl extends NodeEndpointImpl implements LocalNode {

    private final String hostname = "127.0.0.1";
    private final int port = 9090;

    private final CommunicationServer server;

    public LocalNodeImpl(NodeEndpointData data) {
        super(data);

        this.server = new CommunicationServer(hostname, port);
    }

    public void initialize() {
        log.info("Node is listening on @{}:{}", hostname, port);
        this.server.initialize();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LocalNodeImpl localNode && localNode.data().name().equals(data().name());
    }

    @Override
    public void close() {
        this.server.close();
        log.info("Node server successfully shutdown.");
    }
}
