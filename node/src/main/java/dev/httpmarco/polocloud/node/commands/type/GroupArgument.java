package dev.httpmarco.polocloud.node.commands.type;

import dev.httpmarco.polocloud.api.Named;
import dev.httpmarco.polocloud.api.groups.ClusterGroup;
import dev.httpmarco.polocloud.node.commands.CommandArgument;
import dev.httpmarco.polocloud.api.groups.ClusterGroupProvider;
import dev.httpmarco.polocloud.node.commands.CommandContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

public final class GroupArgument extends CommandArgument<ClusterGroup> {

    private final ClusterGroupProvider groupService;

    public GroupArgument(String key, ClusterGroupProvider groupService) {
        super(key);
        this.groupService = groupService;
    }

    @Override
    public boolean predication(@NotNull String rawInput) {
        return groupService.exists(rawInput);
    }

    @Contract(pure = true)
    @Override
    public @NotNull String wrongReason() {
        return "The Argument " + key() + " is not a registered cluster group!";
    }

    @Override
    public @NotNull @Unmodifiable List<String> defaultArgs(CommandContext context) {
        return groupService.groups().stream().map(Named::name).toList();
    }

    @Contract("_ -> new")
    @Override
    public @NotNull ClusterGroup buildResult(String input) {
        return Objects.requireNonNull(groupService.find(input));
    }
}
