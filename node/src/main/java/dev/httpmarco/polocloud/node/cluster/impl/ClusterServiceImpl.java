package dev.httpmarco.polocloud.node.cluster.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.httpmarco.polocloud.node.NodeConfig;
import dev.httpmarco.polocloud.node.cluster.ClusterService;
import dev.httpmarco.polocloud.node.cluster.LocalNode;
import dev.httpmarco.polocloud.node.cluster.NodeEndpoint;
import dev.httpmarco.polocloud.node.cluster.NodeSituation;
import dev.httpmarco.polocloud.node.cluster.tasks.HeadNodeDetection;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

@Log4j2
@Getter
@Accessors(fluent = true)
@Singleton
public final class ClusterServiceImpl implements ClusterService {

    private final LocalNode localNode;
    private NodeEndpoint headNode;
    private final Set<NodeEndpoint> endpoints;

    @Inject
    public ClusterServiceImpl(@NotNull NodeConfig config) {
        this.localNode = new LocalNodeImpl(config.localNode());
        this.endpoints = new HashSet<>();
    }

    public void initialize() {
        // detect head node
        this.headNode = HeadNodeDetection.detect(this);
        log.info("The cluster use {} as the head node&8.", this.headNode.data().name());

        if (!headNode.equals(localNode)) {
            // todo sync
        } else {
            localNode.initialize();
            localNode.situation(NodeSituation.RECHEABLE);
        }
    }

    @Override
    public void close() {
        this.localNode.close();
    }
}
