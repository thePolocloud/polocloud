package dev.httpmarco.polocloud.node.groups.requests;

import dev.httpmarco.osgan.networking.CommunicationProperty;
import dev.httpmarco.polocloud.api.packet.MessageResponsePacket;
import dev.httpmarco.polocloud.api.platforms.PlatformGroupDisplay;
import dev.httpmarco.polocloud.node.cluster.ClusterService;
import dev.httpmarco.polocloud.node.util.JsonUtils;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Log4j2
@UtilityClass
public class GroupCreationRequest {

    public static String TAG = "group-creation";

    public CompletableFuture<Optional<String>> request(@NotNull ClusterService clusterService, String name, String[] nodes, @NotNull PlatformGroupDisplay platform, int minMemory, int maxMemory, boolean staticService, int minOnline, int maxOnline) {
        var groupFuture = new CompletableFuture<Optional<String>>();
        clusterService.headNode().transmit().request(GroupCreationRequest.TAG, new CommunicationProperty()
                        .set("name", name)
                        .set("nodes", JsonUtils.GSON.toJson(nodes))
                        .set("platform", platform.platform())
                        .set("version", platform.version())
                        .set("minMemory", minMemory)
                        .set("maxMemory", maxMemory)
                        .set("staticService", staticService)
                        .set("minOnline", minOnline)
                        .set("maxOnline", maxOnline)
                , MessageResponsePacket.class, packet -> groupFuture.complete(packet.successfully() ? Optional.empty() : Optional.of(packet.reason())));
        return groupFuture;
    }

}
