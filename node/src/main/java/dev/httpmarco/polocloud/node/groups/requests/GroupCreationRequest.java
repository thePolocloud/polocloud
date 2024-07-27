package dev.httpmarco.polocloud.node.groups.requests;

import dev.httpmarco.osgan.networking.CommunicationProperty;
import dev.httpmarco.polocloud.api.groups.ClusterGroup;
import dev.httpmarco.polocloud.api.packet.group.GroupCreationResponse;
import dev.httpmarco.polocloud.api.platforms.PlatformGroupDisplay;
import dev.httpmarco.polocloud.node.cluster.ClusterService;
import dev.httpmarco.polocloud.node.groups.responder.GroupCreationResponder;
import dev.httpmarco.polocloud.node.util.JsonUtils;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.CompletableFuture;

@Log4j2
@UtilityClass
public class GroupCreationRequest {

    public CompletableFuture<ClusterGroup> request(ClusterService clusterService, String name, String[] nodes, PlatformGroupDisplay platform, int minMemory, int maxMemory, boolean staticService, int minOnline, int maxOnline) {
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
                    log.info("{}:{}", packet.successfully(), packet.reason());
                });
        return groupFuture;
    }

}
