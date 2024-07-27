package dev.httpmarco.polocloud.api.groups;

import dev.httpmarco.polocloud.api.Named;
import dev.httpmarco.polocloud.api.platforms.PlatformGroupDisplay;

public interface ClusterGroup extends Named {

    String[] node();

    int minMemory();

    int maxMemory();

    boolean staticService();

    PlatformGroupDisplay platform();

    int minOnlineServerInstances();

    int maxOnlineServerInstances();


}
