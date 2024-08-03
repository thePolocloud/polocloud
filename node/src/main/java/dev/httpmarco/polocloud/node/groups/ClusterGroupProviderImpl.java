package dev.httpmarco.polocloud.node.groups;

import dev.httpmarco.polocloud.api.Named;
import dev.httpmarco.polocloud.api.groups.ClusterGroup;
import dev.httpmarco.polocloud.api.groups.ClusterGroupProvider;
import dev.httpmarco.polocloud.api.packet.group.GroupCreatePacket;
import dev.httpmarco.polocloud.api.packet.group.GroupDeletePacket;
import dev.httpmarco.polocloud.api.platforms.PlatformGroupDisplay;
import dev.httpmarco.polocloud.node.Node;
import dev.httpmarco.polocloud.node.cluster.ClusterService;
import dev.httpmarco.polocloud.node.groups.requests.GroupCreationRequest;
import dev.httpmarco.polocloud.node.groups.requests.GroupDeletionRequest;
import dev.httpmarco.polocloud.node.groups.responder.GroupCreationResponder;
import dev.httpmarco.polocloud.node.groups.responder.GroupDeletionResponder;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
@Accessors(fluent = true)
public final class ClusterGroupProviderImpl extends ClusterGroupProvider {

    private final Set<ClusterGroup> groups;
    private final ClusterService clusterService;

    public ClusterGroupProviderImpl(@NotNull ClusterService clusterService) {
        this.clusterService = clusterService;

        clusterService.localNode().transmit().listen(GroupCreatePacket.class, (transmit, packet) -> ClusterGroupFactory.createLocalStorageGroup(packet, this));
        clusterService.localNode().transmit().listen(GroupDeletePacket.class, (transmit, packet) -> ClusterGroupFactory.deleteLocalStorageGroup(packet.name(), this));

        clusterService.localNode().transmit().responder(GroupCreationRequest.TAG, property -> GroupCreationResponder.handle(this, clusterService, property));
        clusterService.localNode().transmit().responder(GroupDeletionRequest.TAG, property -> GroupDeletionResponder.handle(this, clusterService, property));

        this.groups = ClusterGroupFactory.readGroups();

        if (!groups.isEmpty()) {
            log.info("Loading following groups: {}", String.join(", ", groups.stream().map(Named::name).toList()));
        }
    }

    @Override
    public @NotNull CompletableFuture<Set<ClusterGroup>> groupsAsync() {
        return CompletableFuture.completedFuture(groups);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> existsAsync(String group) {
        return CompletableFuture.completedFuture(this.groups.stream().anyMatch(it -> it.name().equalsIgnoreCase(group)));
    }

    @Contract(pure = true)
    @Override
    public @Nullable CompletableFuture<Optional<String>> deleteAsync(String group) {
        return GroupDeletionRequest.request(clusterService, group);
    }

    @Override
    public CompletableFuture<Optional<String>> createAsync(String name, String[] nodes, PlatformGroupDisplay platform, int minMemory, int maxMemory, boolean staticService, int minOnline, int maxOnline) {
        return GroupCreationRequest.request(clusterService, name, nodes, platform, minMemory, maxMemory, staticService, minOnline, maxOnline);
    }

    @Override
    public @NotNull CompletableFuture<ClusterGroup> findAsync(@NotNull String group) {
        return CompletableFuture.completedFuture(this.groups.stream().filter(it -> it.name().equalsIgnoreCase(group)).findFirst().orElse(null));
    }

    @Override
    public void reload() {

        this.groups.clear();
        if (Node.instance().clusterService().localHead()) {
            this.groups.addAll(ClusterGroupFactory.readGroups());
        } else {
            //todo request groups from head
        }

        log.info("Successfully reload all group data.");
    }
}
