package dev.httpmarco.polocloud.node.groups;

import dev.httpmarco.polocloud.api.groups.ClusterGroup;

public interface FallbackClusterGroup extends ClusterGroup {

    boolean fallback();

}
