package dev.httpmarco.polocloud.node.commands;

import dev.httpmarco.polocloud.api.groups.ClusterGroup;
import dev.httpmarco.polocloud.node.commands.type.GroupArgument;
import dev.httpmarco.polocloud.node.commands.type.IntArgument;
import dev.httpmarco.polocloud.node.commands.type.KeywordArgument;
import dev.httpmarco.polocloud.node.commands.type.TextArgument;
import dev.httpmarco.polocloud.api.groups.ClusterGroupService;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public final class CommandArgumentType {

    public @NotNull CommandArgument<ClusterGroup> ClusterGroup(ClusterGroupService groupService, String key) {
        return new GroupArgument(key, groupService);
    }

    @Contract("_ -> new")
    public @NotNull CommandArgument<Integer> Integer(String key) {
        return new IntArgument(key);
    }

    @Contract("_ -> new")
    public @NotNull CommandArgument<String> Text(String key) {
        return new TextArgument(key);
    }

    @Contract("_ -> new")
    public @NotNull CommandArgument<String> Keyword(String key) {
        return new KeywordArgument(key);
    }
}
