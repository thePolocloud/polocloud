package dev.httpmarco.polocloud.api.packets;

import dev.httpmarco.osgan.networking.Packet;
import dev.httpmarco.osgan.networking.codec.CodecBuffer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Accessors(fluent = true)
public class CloudServiceRegisterPacket extends Packet {

    private UUID uuid;

    public CloudServiceRegisterPacket(UUID uuid) {
        this.uuid = uuid;
        getBuffer().writeUniqueId(this.uuid);
    }

    public CloudServiceRegisterPacket(CodecBuffer buffer) {
        super(buffer);

        this.uuid = buffer.readUniqueId();
    }
}
