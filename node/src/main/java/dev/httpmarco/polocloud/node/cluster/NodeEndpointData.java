package dev.httpmarco.polocloud.node.cluster;


import dev.httpmarco.polocloud.api.Detail;

public record NodeEndpointData(String name, String hostname, int port) implements Detail {

    @Override
    public String details() {
        return "hostname&8=&7" + this.hostname + "&8, &7port&8=&7" + this.port;
    }
}
