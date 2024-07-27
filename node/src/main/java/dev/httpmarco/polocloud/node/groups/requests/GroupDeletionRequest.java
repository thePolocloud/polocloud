package dev.httpmarco.polocloud.node.groups.requests;

import dev.httpmarco.osgan.networking.CommunicationProperty;
import dev.httpmarco.polocloud.api.packet.MessageResponsePacket;
import dev.httpmarco.polocloud.node.cluster.ClusterService;
import lombok.experimental.UtilityClass;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@UtilityClass
public class GroupDeletionRequest {

    public static String TAG = "group-deletion";

    public CompletableFuture<Optional<String>> request(ClusterService clusterService, String name) {
        var groupFuture = new CompletableFuture<Optional<String>>();

        clusterService.headNode().transmit().request(GroupDeletionRequest.TAG, new CommunicationProperty().set("name", name), MessageResponsePacket.class,
                packet -> groupFuture.complete(packet.successfully() ? Optional.empty() : Optional.of(packet.reason()))
        );

        return groupFuture;
    }
}
