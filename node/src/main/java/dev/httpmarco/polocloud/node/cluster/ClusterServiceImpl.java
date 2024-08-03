package dev.httpmarco.polocloud.node.cluster;

import dev.httpmarco.osgan.networking.packet.Packet;
import dev.httpmarco.polocloud.node.Node;
import dev.httpmarco.polocloud.node.NodeConfig;
import dev.httpmarco.polocloud.node.cluster.impl.LocalNodeImpl;
import dev.httpmarco.polocloud.node.cluster.tasks.HeadNodeDetection;
import dev.httpmarco.polocloud.node.packets.ClusterReloadCallPacket;
import dev.httpmarco.polocloud.node.packets.ClusterRequireReloadPacket;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

@Log4j2
@Getter
@Accessors(fluent = true)
public final class ClusterServiceImpl implements ClusterService {

    private final LocalNode localNode;
    private NodeEndpoint headNode;
    private final Set<NodeEndpoint> endpoints;

    public ClusterServiceImpl(@NotNull NodeConfig config) {
        this.localNode = new LocalNodeImpl(config.localNode());
        this.endpoints = new HashSet<>();


        localNode.transmit().listen(ClusterRequireReloadPacket.class, (transmit, packet) -> broadcastAll(new ClusterReloadCallPacket()));
        localNode.transmit().listen(ClusterReloadCallPacket.class, (transmit, packet) -> {

            // reloading first all groups
            Node.instance().groupService().reload();
        });
    }

    @Override
    public boolean localHead() {
        return this.headNode.equals(localNode);
    }

    @Override
    public void broadcast(Packet packet) {
        for (var endpoint : this.endpoints) {
            endpoint.transmit().sendPacket(packet);
        }
    }

    @Override
    public void broadcastAll(Packet packet) {
        this.localNode.transmit().sendPacket(packet);
        for (var endpoint : this.endpoints) {
            endpoint.transmit().sendPacket(packet);
        }
    }


    public void initialize() {
        // detect head node
        this.headNode = HeadNodeDetection.detect(this);
        log.info("The cluster use {} as the head node&8.", this.headNode.data().name());

        if (!headNode.equals(localNode)) {
            // todo sync
        } else {
            localNode.initialize();
            localNode.situation(NodeSituation.RECHEABLE);
        }
    }

    @Override
    public void close() {
        this.localNode.close();
    }
}
