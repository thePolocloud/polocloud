package dev.httpmarco.polocloud.api.packet.group;

import dev.httpmarco.osgan.networking.packet.Packet;
import dev.httpmarco.osgan.networking.packet.PacketBuffer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public final class GroupDeletePacket extends Packet {

    private String name;

    @Override
    public void read(@NotNull PacketBuffer packetBuffer) {
        this.name = packetBuffer.readString();
    }

    @Override
    public void write(@NotNull PacketBuffer packetBuffer) {
        packetBuffer.writeString(name);
    }
}
