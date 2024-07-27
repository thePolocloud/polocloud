package dev.httpmarco.polocloud.api.groups;

import dev.httpmarco.polocloud.api.platforms.PlatformGroupDisplay;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface ClusterGroupService {

    Set<ClusterGroup> groups();

    boolean exists(String group);

    CompletableFuture<Optional<String>> delete(String group);

    CompletableFuture<ClusterGroup> create(String name, String[] nodes, PlatformGroupDisplay platform, int minMemory, int maxMemory, boolean staticService, int minOnline, int maxOnline);

}
