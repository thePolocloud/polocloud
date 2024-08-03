package dev.httpmarco.polocloud.api.groups;

import dev.httpmarco.polocloud.api.Detail;
import dev.httpmarco.polocloud.api.Named;
import dev.httpmarco.polocloud.api.platforms.PlatformGroupDisplay;
import dev.httpmarco.polocloud.api.services.ClusterService;

import java.util.List;

public interface ClusterGroup extends Named, Detail {

    String[] nodes();

    int minMemory();

    int maxMemory();

    boolean staticService();

    PlatformGroupDisplay platform();

    int minOnlineServerInstances();

    int maxOnlineServerInstances();

    long serviceCount();

    List<ClusterService> services();

}
