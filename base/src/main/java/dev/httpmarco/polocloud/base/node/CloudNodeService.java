package dev.httpmarco.polocloud.base.node;

import dev.httpmarco.osgan.files.Document;
import dev.httpmarco.osgan.files.json.JsonDocument;
import dev.httpmarco.polocloud.api.node.NodeService;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.nio.file.Path;

@Getter
@Accessors(fluent = true)
public final class CloudNodeService implements NodeService {

    private final Document<NodeConfiguration> configuration;
    private final LocalNode localNode;

    public CloudNodeService() {
        this.configuration = new JsonDocument<>(new NodeConfiguration(), Path.of("local/node.json"));
        this.localNode = this.configuration.value().localNode();
    }
}
