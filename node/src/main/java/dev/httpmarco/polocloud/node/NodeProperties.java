package dev.httpmarco.polocloud.node;

import dev.httpmarco.polocloud.api.properties.Property;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class NodeProperties {

    // define the start port of all proxies on this node. Default: 25565
    public final Property<Integer> PROXY_PORT_START_RANGE = Property.of("PROXY_PORT_START_RANGE", Integer.class);

    // define the start port of all servers (spigot, paper...) on this node. Default: 30000
    public final Property<Integer> SERVER_PORT_START_RANGE = Property.of("SERVER_PORT_START_RANGE", Integer.class);

    // define the start port of all services (multi-paper master) on this node. Default: 25565
    public final Property<Integer> SERVICE_PORT_START_RANGE = Property.of("SERVICE_PORT_START_RANGE", Integer.class);

    // define the start port of all servers (spigot, paper...) of the group. Default: platform type default port
    public final Property<Integer> GROUP_PORT_START_RANGE = Property.of("GROUP_PORT_START_RANGE", Integer.class);

    // if a player connect the cluster with a proxy, we notify this in the terminal
    public final Property<Boolean> LOG_PLAYERS_CONNECTION = Property.of("LOG_PLAYERS_CONNECTION", Boolean.class);

    // if a player disconnect the cluster with a proxy, we notify this in the terminal
    public final Property<Boolean> LOG_PLAYERS_DISCONNECTION = Property.of("LOG_PLAYERS_DISCONNECTION", Boolean.class);

    // if a player switch the server, we notify this in the terminal
    public final Property<Boolean> LOG_PLAYERS_SERVER_SWITCH = Property.of("LOG_PLAYERS_SERVER_SWITCH", Boolean.class);

}
