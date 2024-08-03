package dev.httpmarco.polocloud.node.cluster.impl;

import dev.httpmarco.osgan.networking.server.CommunicationServer;
import dev.httpmarco.polocloud.node.cluster.LocalNode;
import dev.httpmarco.polocloud.node.cluster.NodeEndpointData;
import dev.httpmarco.polocloud.node.cluster.impl.transmit.LocalChannelTransmit;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Getter
@Accessors(fluent = true)
public final class LocalNodeImpl extends NodeEndpointImpl implements LocalNode {

    private final String hostname = "127.0.0.1";
    private final int port = 9090;

    private final CommunicationServer server;
    private final LocalChannelTransmit transmit;

    public LocalNodeImpl(NodeEndpointData data) {
        super(data);

        this.server = new CommunicationServer(hostname, port);
        this.transmit = new LocalChannelTransmit(server);
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
