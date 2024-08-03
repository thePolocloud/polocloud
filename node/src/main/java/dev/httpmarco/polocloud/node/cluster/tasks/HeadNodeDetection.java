package dev.httpmarco.polocloud.node.cluster.tasks;

import dev.httpmarco.polocloud.node.cluster.NodeEndpoint;
import dev.httpmarco.polocloud.node.cluster.NodeSituation;
import dev.httpmarco.polocloud.node.cluster.ClusterServiceImpl;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class HeadNodeDetection {

    public NodeEndpoint detect(@NotNull ClusterServiceImpl service) {

        if (service.endpoints().isEmpty()) {
            return service.localNode();
        }

        // todo connect with other nodes

        for (NodeEndpoint endpoint : service.endpoints()) {

            if (endpoint.situation() == NodeSituation.RECHEABLE) {
                // request head node data
            }
        }

        return service.localNode();
    }
}
