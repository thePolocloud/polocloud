package dev.httpmarco.polocloud.api.services;

import dev.httpmarco.osgan.networking.packet.PacketBuffer;
import dev.httpmarco.polocloud.api.Sendable;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class ClusterServiceProvider implements Sendable<ClusterService> {

    @SneakyThrows
    public List<ClusterService> services() {
        return servicesAsync().get(5, TimeUnit.SECONDS);
    }

    public abstract CompletableFuture<List<ClusterService>> servicesAsync();

    public abstract ClusterServiceFactory factory();

    @Override
    public void write(@NotNull ClusterService value, @NotNull PacketBuffer buffer) {
        buffer.writeUniqueId(value.id());
        buffer.writeInt(value.orderedId());
        buffer.writeString(value.hostname());
        buffer.writeInt(value.port());
    }
}
