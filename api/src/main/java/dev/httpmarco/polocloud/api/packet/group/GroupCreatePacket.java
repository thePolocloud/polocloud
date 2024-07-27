package dev.httpmarco.polocloud.api.packet.group;

import dev.httpmarco.osgan.networking.packet.Packet;
import dev.httpmarco.osgan.networking.packet.PacketBuffer;
import dev.httpmarco.polocloud.api.platforms.PlatformGroupDisplay;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

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
    public void read(@NotNull PacketBuffer packetBuffer) {
        this.name = packetBuffer.readString();

        var nodeSide = packetBuffer.readInt();
        var nodes = new HashSet<String>();

        for (int i = 0; i < nodeSide; i++) {
            nodes.add(packetBuffer.readString());
        }
        this.nodes = nodes.toArray(String[]::new);

        var platform = packetBuffer.readString();
        var version = packetBuffer.readString();
        this.platformGroupDisplay = new PlatformGroupDisplay(platform, version);

        this.minMemory = packetBuffer.readInt();
        this.maxMemory = packetBuffer.readInt();
        this.staticService = packetBuffer.readBoolean();
        this.minOnline = packetBuffer.readInt();
        this.maxOnline = packetBuffer.readInt();

    }

    @Override
    public void write(@NotNull PacketBuffer packetBuffer) {
        packetBuffer.writeString(name);
        packetBuffer.writeInt(nodes.length);

        for (String node : nodes) {
            packetBuffer.writeString(node);
        }

        packetBuffer.writeString(platformGroupDisplay.platform());
        packetBuffer.writeString(platformGroupDisplay.version());

        packetBuffer.writeInt(minMemory);
        packetBuffer.writeInt(maxMemory);
        packetBuffer.writeBoolean(staticService);
        packetBuffer.writeInt(minOnline);
        packetBuffer.writeInt(maxOnline);
    }
}
