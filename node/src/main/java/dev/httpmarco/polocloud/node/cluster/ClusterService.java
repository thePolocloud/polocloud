package dev.httpmarco.polocloud.node.cluster;

import dev.httpmarco.osgan.networking.packet.Packet;
import dev.httpmarco.polocloud.api.Closeable;

import java.util.Set;

public interface ClusterService extends Closeable {

    LocalNode localNode();

    NodeEndpoint headNode();

    boolean localHead();

    void broadcast(Packet packet);

    void broadcastAll(Packet packet);

    Set<NodeEndpoint> endpoints();

    void initialize();

}
