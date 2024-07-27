package dev.httpmarco.polocloud.node.groups;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.httpmarco.polocloud.api.groups.ClusterGroup;
import dev.httpmarco.polocloud.api.groups.ClusterGroupService;
import dev.httpmarco.polocloud.api.packet.group.GroupCreatePacket;
import dev.httpmarco.polocloud.api.platforms.PlatformGroupDisplay;
import dev.httpmarco.polocloud.node.cluster.ClusterService;
import dev.httpmarco.polocloud.node.groups.requests.GroupCreationRequest;
import dev.httpmarco.polocloud.node.groups.responder.GroupCreationResponder;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
@Singleton
@Accessors(fluent = true)
public final class ClusterGroupServiceImpl implements ClusterGroupService {

    private final Set<ClusterGroup> groups = new HashSet<>();

    private final ClusterService clusterService;

    @Inject
    public ClusterGroupServiceImpl(@NotNull ClusterService clusterService) {
        this.clusterService = clusterService;

        clusterService.localNode().transmit().responder(GroupCreationResponder.TAG, property -> GroupCreationResponder.handle(this, property));

        // if the head node alert that the cluster has a new node
        clusterService.localNode().transmit().listen(GroupCreatePacket.class, (packet) -> {
            //  ClusterNodeGroupCreateProcess.create(packet.name(), packet.nodes(), packet.platformGroupDisplay(), packet.minMemory(), packet.maxMemory(), packet.staticService(), packet.minOnline(), packet.maxOnline());
        });
    }

    @Override
    public boolean exists(String group) {
        return this.groups.stream().anyMatch(it -> it.name().equalsIgnoreCase(group));
    }

    @Contract(pure = true)
    @Override
    public @Nullable CompletableFuture<Optional<String>> delete(String group) {
        return null;
    }

    @Override
    public CompletableFuture<ClusterGroup> create(String name, String[] nodes, PlatformGroupDisplay platform, int minMemory, int maxMemory, boolean staticService, int minOnline, int maxOnline) {
        return GroupCreationRequest.request(clusterService, name, nodes, platform, minMemory, maxMemory, staticService, minOnline, maxOnline);
    }
}
