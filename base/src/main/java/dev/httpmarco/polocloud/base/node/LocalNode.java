package dev.httpmarco.polocloud.base.node;

import dev.httpmarco.osgan.files.annotations.ConfigExclude;
import dev.httpmarco.osgan.networking.server.NettyServer;
import dev.httpmarco.polocloud.api.CloudAPI;
import dev.httpmarco.polocloud.api.node.AbstractNode;
import dev.httpmarco.polocloud.api.packets.CloudServiceRegisterPacket;

import java.util.UUID;

public final class LocalNode extends AbstractNode implements dev.httpmarco.polocloud.api.node.LocalNode {

    @ConfigExclude
    private NettyServer server;

    public LocalNode(UUID id, String name, String hostname, int port) {
        super(id, name, hostname, port);
    }

    public void initialize() {
        server = NettyServer.builder().onInactive(channelTransmit -> CloudAPI.instance().logger().info("Communication instance is successfully stopped!")).build();

        server.listen(CloudServiceRegisterPacket.class, (channelTransmit, cloudServiceRegisterPacket) -> {
            var service = CloudAPI.instance().serviceProvider().find(cloudServiceRegisterPacket.uuid());

            CloudAPI.instance().logger().info("Server " + service.name() + " is now successfully started up&2.");
        });
    }

    @Override
    public void close() {
        if (this.server != null) {
            this.server.close();
        }
    }
}
