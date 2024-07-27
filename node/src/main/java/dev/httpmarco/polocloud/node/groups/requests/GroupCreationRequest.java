package dev.httpmarco.polocloud.node.groups.requests;

import dev.httpmarco.osgan.networking.CommunicationProperty;
import dev.httpmarco.polocloud.api.groups.ClusterGroup;
import dev.httpmarco.polocloud.api.groups.ClusterGroupService;
import dev.httpmarco.polocloud.api.packet.group.GroupCreationResponse;
import dev.httpmarco.polocloud.api.platforms.PlatformGroupDisplay;
import dev.httpmarco.polocloud.node.cluster.ClusterService;
import dev.httpmarco.polocloud.node.groups.responder.GroupCreationResponder;
import dev.httpmarco.polocloud.node.util.JsonUtils;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@Log4j2
@UtilityClass
public class GroupCreationRequest {

    public CompletableFuture<ClusterGroup> request(ClusterGroupService groupService, @NotNull ClusterService clusterService, String name, String[] nodes, @NotNull PlatformGroupDisplay platform, int minMemory, int maxMemory, boolean staticService, int minOnline, int maxOnline) {
        var groupFuture = new CompletableFuture<ClusterGroup>();
        clusterService.headNode().transmit().request(GroupCreationResponder.TAG, new CommunicationProperty()
                        .set("name", name)
                        .set("nodes", JsonUtils.GSON.toJson(nodes))
                        .set("platform", platform.platform())
                        .set("version", platform.version())
                        .set("minMemory", minMemory)
                        .set("maxMemory", maxMemory)
                        .set("staticService", staticService)
                        .set("minOnline", minOnline)
                        .set("maxOnline", maxOnline)
                , GroupCreationResponse.class, packet -> {
                    if (packet.successfully()) {
                        groupFuture.complete(groupService.find(name));
                    } else {
                        groupFuture.completeExceptionally(new Throwable(packet.reason()));
                    }
                });
        return groupFuture;
    }

}
