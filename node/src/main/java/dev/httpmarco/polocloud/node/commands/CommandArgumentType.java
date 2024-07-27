package dev.httpmarco.polocloud.node.commands;

import dev.httpmarco.polocloud.api.groups.ClusterGroup;
import dev.httpmarco.polocloud.node.commands.type.*;
import dev.httpmarco.polocloud.api.groups.ClusterGroupService;
import dev.httpmarco.polocloud.node.platforms.Platform;
import dev.httpmarco.polocloud.node.platforms.PlatformService;
import dev.httpmarco.polocloud.node.platforms.PlatformVersion;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public final class CommandArgumentType {

    public @NotNull CommandArgument<ClusterGroup> ClusterGroup(ClusterGroupService groupService, String key) {
        return new GroupArgument(key, groupService);
    }

    public @NotNull CommandArgument<PlatformVersion> PlatformVersion(PlatformService platformService, String key) {
        return new PlatformVersionArgument(key, platformService);
    }

    public @NotNull CommandArgument<Platform> Platform(PlatformService platformService, String key) {
        return new PlatformArgument(key, platformService);
    }


    @Contract("_ -> new")
    public @NotNull CommandArgument<Integer> Integer(String key) {
        return new IntArgument(key);
    }

    @Contract("_ -> new")
    public @NotNull CommandArgument<Boolean> Boolean(String key) {
        return new BooleanArgument(key);
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
