package dev.httpmarco.polocloud.api.packets.resources.services;

import dev.httpmarco.osgan.networking.packet.Packet;
import dev.httpmarco.osgan.networking.packet.PacketBuffer;
import dev.httpmarco.polocloud.api.CloudAPI;
import dev.httpmarco.polocloud.api.services.ClusterService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Getter
@Accessors(fluent = true)
public abstract class AbstractClusterServicePacket extends Packet {

    private ClusterService service;

    @Override
    public void read(PacketBuffer packetBuffer) {
        this.service = CloudAPI.instance().serviceProvider().read(packetBuffer);
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        CloudAPI.instance().serviceProvider().write(service, packetBuffer);
    }
}
