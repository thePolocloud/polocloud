package dev.httpmarco.polocloud.api;

import dev.httpmarco.polocloud.api.dependencies.DependencyService;
import dev.httpmarco.polocloud.api.events.Event;
import dev.httpmarco.polocloud.api.events.EventNode;
import dev.httpmarco.polocloud.api.groups.CloudGroupProvider;
import dev.httpmarco.polocloud.api.logging.Logger;
import dev.httpmarco.polocloud.api.logging.LoggerFactory;
import dev.httpmarco.polocloud.api.node.NodeService;
import dev.httpmarco.polocloud.api.services.CloudServiceProvider;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public abstract class CloudAPI {

    @Getter
    private static CloudAPI instance;

    private final LoggerFactory loggerFactory = new LoggerFactory();
    private final Logger logger = new Logger();

    private final DependencyService dependencyService = new DependencyService();

    @Getter
    private final EventNode<Event> globalEventHandler = new EventNode<Event>();

    public CloudAPI() {
        instance = this;
    }

    public abstract NodeService nodeService();

    public abstract CloudGroupProvider groupProvider();
    public abstract CloudServiceProvider serviceProvider();

}
