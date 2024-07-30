package dev.httpmarco.polocloud.node.groups;

import dev.httpmarco.polocloud.node.commands.CommandArgument;
import dev.httpmarco.polocloud.node.commands.type.BooleanArgument;
import dev.httpmarco.polocloud.node.commands.type.IntArgument;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ClusterGroupEditFields {

    STATIC(new BooleanArgument("static")),
    MIN_ONLINE_SERVICES(new IntArgument("minOnlineServices")),
    MAX_ONLINE_SERVICES(new IntArgument("maxOnlineServices")),
    MIN_MEMORY(new IntArgument("minMemory")),
    MAX_MEMORY(new IntArgument("maxMemory")),
  //  PLATFORM(new PlatformArgument("", "")),
    //PLATFORM_VERSION,
    FALLBACK(new BooleanArgument("fallback"));
    // NODES

    private final CommandArgument<?> argument;

}
