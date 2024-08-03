package dev.httpmarco.polocloud.api.services;

import dev.httpmarco.polocloud.api.groups.ClusterGroup;

public interface ClusterServiceFactory {

    void runGroupService(ClusterGroup group);

    void shutdownGroupService(ClusterService clusterService);

}
