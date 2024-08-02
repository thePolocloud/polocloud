package dev.httpmarco.polocloud.node.services;

import dev.httpmarco.polocloud.api.groups.ClusterGroup;
import dev.httpmarco.polocloud.node.Node;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import java.util.Arrays;

@Log4j2
public final class ClusterServiceQueue extends Thread {

    public ClusterServiceQueue() {
        super("cluster-node-service-queue");
    }

    @SneakyThrows
    @Override
    public void run() {

        var groupService = Node.instance().groupService();
        var localNode = Node.instance().clusterService().localNode();

        while (!localNode.situation().isStopping()) {

            if(Node.instance().clusterService().headNode() == null) {
                // some cloud ticks is the head node null for new calculation
                continue;
            }

            if(!Node.instance().clusterService().localHead()) {
                continue;
            }

            for (ClusterGroup group : groupService.groups()) {

                if (Arrays.stream(group.nodes()).noneMatch(it -> it.equalsIgnoreCase(localNode.data().name()))) {
                    continue;
                }

                if (group.serviceCount() >= group.maxOnlineServerInstances()) {
                    continue;
                }

                var differenceGroupServices = group.minOnlineServerInstances() - group.serviceCount();

                for (long i = 0; i < differenceGroupServices; i++) {
                    Node.instance().serviceProvider().factory().runGroupService(group);
                }
            }
            Thread.sleep(5000);
        }
    }
}
