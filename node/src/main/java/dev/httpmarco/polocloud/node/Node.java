package dev.httpmarco.polocloud.node;

import com.google.inject.Inject;
import dev.httpmarco.polocloud.node.cluster.ClusterService;
import dev.httpmarco.polocloud.node.terminal.JLineTerminal;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

@Log4j2
@Accessors(fluent = true)
public final class Node {

    @Inject
    public Node(@NotNull ClusterService cluster, @NotNull JLineTerminal terminal) {
        // start cluster and check other node
        cluster.initialize();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> NodeShutdown.nodeShutdown(terminal, cluster)));

        log.info("Cluster node boot successfully &8(&7Took {}ms&8)", System.currentTimeMillis() - Long.parseLong(System.getProperty("startup")));
        terminal.allowInput();
    }
}
