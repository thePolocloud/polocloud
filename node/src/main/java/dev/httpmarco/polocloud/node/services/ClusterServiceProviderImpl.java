package dev.httpmarco.polocloud.node.services;

import dev.httpmarco.osgan.networking.packet.PacketBuffer;
import dev.httpmarco.polocloud.api.services.ClusterService;
import dev.httpmarco.polocloud.api.services.ClusterServiceFactory;
import dev.httpmarco.polocloud.api.services.ClusterServiceProvider;
import dev.httpmarco.polocloud.node.Node;
import dev.httpmarco.polocloud.node.packets.resources.services.ClusterSyncRegisterServicePacket;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Log4j2
@Getter
@Accessors(fluent = true)
public final class ClusterServiceProviderImpl extends ClusterServiceProvider {

    private final List<ClusterService> services = new ArrayList<>();
    private final ClusterServiceFactory factory = new ClusterServiceFactoryImpl();
    private final ClusterServiceQueue clusterServiceQueue = new ClusterServiceQueue();

    public ClusterServiceProviderImpl() {
        Node.instance().clusterService().localNode().transmit().listen(ClusterSyncRegisterServicePacket.class, (it, packet) -> {
            services.add(packet.service());
            log.info("The service &8'&f{}&8' &7is starting now&8...", packet.service().name());
        });
    }

    @Override
    public @NotNull CompletableFuture<List<ClusterService>> servicesAsync() {
        return CompletableFuture.completedFuture(services);
    }

    @Override
    public ClusterService read(PacketBuffer buffer) {
        return null;
    }
}
