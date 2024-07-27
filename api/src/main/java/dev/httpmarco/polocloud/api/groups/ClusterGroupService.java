package dev.httpmarco.polocloud.api.groups;

import dev.httpmarco.polocloud.api.platforms.PlatformGroupDisplay;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class ClusterGroupService {

    public abstract Set<ClusterGroup> groups();

    public abstract boolean exists(String group);

    public abstract CompletableFuture<Optional<String>> delete(String group);

    public abstract CompletableFuture<ClusterGroup> create(String name, String[] nodes, PlatformGroupDisplay platform, int minMemory, int maxMemory, boolean staticService, int minOnline, int maxOnline);


    public abstract CompletableFuture<ClusterGroup> findAsync(@NotNull String group);

    @SneakyThrows
    public @Nullable ClusterGroup find(@NotNull String group) {
        return this.findAsync(group).get(5, TimeUnit.SECONDS);
    }
}
