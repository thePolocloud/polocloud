package dev.httpmarco.polocloud.node;

import com.google.inject.AbstractModule;
import dev.httpmarco.polocloud.node.cluster.ClusterService;
import dev.httpmarco.polocloud.node.cluster.impl.ClusterServiceImpl;
import dev.httpmarco.polocloud.node.commands.CommandService;
import dev.httpmarco.polocloud.node.commands.CommandServiceImpl;
import dev.httpmarco.polocloud.api.groups.ClusterGroupService;
import dev.httpmarco.polocloud.node.groups.ClusterGroupServiceImpl;
import dev.httpmarco.polocloud.node.platforms.PlatformService;
import dev.httpmarco.polocloud.node.terminal.JLineTerminal;
import dev.httpmarco.polocloud.node.util.Configurations;

import java.nio.file.Path;

public final class NodeModule extends AbstractModule {

    @Override
    protected void configure() {

        // inject configuration
        var nodeConfig = Configurations.readContent(Path.of("config.json"), new NodeConfig());
        bind(NodeConfig.class).toInstance(nodeConfig);

        // terminal and logs
        bind(JLineTerminal.class).asEagerSingleton();
        bind(CommandService.class).to(CommandServiceImpl.class).asEagerSingleton();
        bind(PlatformService.class).asEagerSingleton();

        // load global handler
        bind(ClusterService.class).to(ClusterServiceImpl.class);
        bind(ClusterGroupService.class).to(ClusterGroupServiceImpl.class);
    }
}
