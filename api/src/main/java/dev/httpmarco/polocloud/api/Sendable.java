package dev.httpmarco.polocloud.api;

import dev.httpmarco.osgan.networking.packet.PacketBuffer;

public interface Sendable<T> {

    void write(T value, PacketBuffer buffer);

    T read(PacketBuffer buffer);

}
