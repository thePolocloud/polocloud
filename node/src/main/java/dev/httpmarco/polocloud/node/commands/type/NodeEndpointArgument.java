package dev.httpmarco.polocloud.node.commands.type;

import dev.httpmarco.polocloud.node.cluster.ClusterService;
import dev.httpmarco.polocloud.node.cluster.NodeEndpoint;
import dev.httpmarco.polocloud.node.commands.CommandArgument;
import dev.httpmarco.polocloud.node.commands.CommandContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public final class NodeEndpointArgument extends CommandArgument<NodeEndpoint> {

    private final ClusterService clusterService;

    public NodeEndpointArgument(String key, ClusterService clusterService) {
        super(key);
        this.clusterService = clusterService;
    }

    @Override
    public boolean predication(@NotNull String rawInput) {
        return clusterService.endpoints().stream().anyMatch(nodeEndpoint -> nodeEndpoint.data().name().equalsIgnoreCase(rawInput));
    }

    @Contract(pure = true)
    @Override
    public @NotNull String wrongReason() {
        return "The Argument " + key() + " is not a registered node endpoint!";
    }

    @Override
    public @NotNull @Unmodifiable List<String> defaultArgs(CommandContext context) {
        return clusterService.endpoints().stream().map(endpoint -> endpoint.data().name()).toList();
    }

    @Contract("_ -> new")
    @Override
    public @NotNull NodeEndpoint buildResult(String input) {
        return clusterService.endpoints()
                .stream()
                .filter(endpoint -> endpoint.data().name().equalsIgnoreCase(input))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(this.wrongReason()));
    }
}
