package dev.httpmarco.polocloud.node.groups;

import com.google.inject.Singleton;
import dev.httpmarco.polocloud.api.groups.ClusterGroup;
import dev.httpmarco.polocloud.api.groups.ClusterGroupService;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Getter
@Singleton
@Accessors(fluent = true)
public final class ClusterGroupServiceImpl implements ClusterGroupService {

    private final Set<ClusterGroup> groups = new HashSet<>();

    @Override
    public boolean exists(String group) {
        return this.groups.stream().anyMatch(it -> it.name().equalsIgnoreCase(group));
    }

    @Override
    public CompletableFuture<Optional<String>> delete(String group) {
        return null;
    }
}
