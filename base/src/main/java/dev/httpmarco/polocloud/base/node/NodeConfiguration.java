package dev.httpmarco.polocloud.base.node;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.UUID;

@Getter
@Accessors(fluent = true)
public final class NodeConfiguration {

    private final LocalNode localNode;
    private final ExternalNode[] externalNodes;

    public NodeConfiguration() {
        // todo if connect with external node -> warning if localhost
        this.localNode = new LocalNode(UUID.randomUUID(), "node-1", "127.0.0.1", 8192);
        this.externalNodes = new ExternalNode[0];
    }
}
