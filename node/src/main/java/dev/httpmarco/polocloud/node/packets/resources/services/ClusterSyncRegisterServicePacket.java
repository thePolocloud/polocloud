package dev.httpmarco.polocloud.node.packets.resources.services;

import dev.httpmarco.polocloud.api.packets.resources.services.AbstractClusterServicePacket;
import dev.httpmarco.polocloud.api.services.ClusterService;

public final class ClusterSyncRegisterServicePacket extends AbstractClusterServicePacket {

    public ClusterSyncRegisterServicePacket(ClusterService service) {
        super(service);
    }
}
