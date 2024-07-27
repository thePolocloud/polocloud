package dev.httpmarco.polocloud.api.packet.group;

import dev.httpmarco.osgan.networking.packet.Packet;
import dev.httpmarco.osgan.networking.packet.PacketBuffer;
import dev.httpmarco.polocloud.api.platforms.PlatformGroupDisplay;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public final class GroupCreatePacket extends Packet {

    private String name;
    private String[] nodes;
    private PlatformGroupDisplay platformGroupDisplay;
    private int minMemory;
    private int maxMemory;
    private boolean staticService;
    private int minOnline;
    private int maxOnline;

    @Override
    public void read(PacketBuffer packetBuffer) {

    }

    @Override
    public void write(PacketBuffer packetBuffer) {

    }
}
