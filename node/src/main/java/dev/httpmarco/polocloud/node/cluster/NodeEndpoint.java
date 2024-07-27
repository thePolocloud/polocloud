package dev.httpmarco.polocloud.node.cluster;

import dev.httpmarco.osgan.networking.channel.ChannelTransmit;
import dev.httpmarco.polocloud.api.Closeable;

public interface NodeEndpoint extends Closeable {

    NodeSituation situation();

    NodeEndpointData data();

    ChannelTransmit transmit();

    void situation(NodeSituation situation);

}
