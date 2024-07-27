package dev.httpmarco.polocloud.api.packet;

import dev.httpmarco.osgan.networking.packet.Packet;
import dev.httpmarco.osgan.networking.packet.PacketBuffer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public final class MessageResponsePacket extends Packet {

    private boolean successfully;
    private String reason;

    @Override
    public void read(@NotNull PacketBuffer packetBuffer) {
        this.successfully = packetBuffer.readBoolean();

        if (!this.successfully) {
            this.reason = packetBuffer.readString();
        }
    }

    @Override
    public void write(@NotNull PacketBuffer packetBuffer) {
        packetBuffer.writeBoolean(this.successfully);

        if (!successfully) {
            packetBuffer.writeString(reason);
        }
    }

    @Contract("_ -> new")
    public static @NotNull MessageResponsePacket fail(String reason) {
        return new MessageResponsePacket(false, reason);
    }

    @Contract(" -> new")
    public static @NotNull MessageResponsePacket success() {
        return new MessageResponsePacket(true, null);
    }

}
