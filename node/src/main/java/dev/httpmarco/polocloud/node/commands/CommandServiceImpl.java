package dev.httpmarco.polocloud.node.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.httpmarco.polocloud.node.cluster.ClusterService;
import dev.httpmarco.polocloud.api.groups.ClusterGroupService;
import dev.httpmarco.polocloud.node.platforms.PlatformService;
import dev.httpmarco.polocloud.node.terminal.JLineTerminal;
import dev.httpmarco.polocloud.node.terminal.commands.ClearCommand;
import dev.httpmarco.polocloud.node.terminal.commands.GroupCommand;
import dev.httpmarco.polocloud.node.terminal.commands.ShutdownCommand;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Accessors(fluent = true)
@Singleton
public final class CommandServiceImpl implements CommandService {

    private final List<Command> commands = new ArrayList<>();

    // todo search for a better inject way
    @Inject
    public CommandServiceImpl(JLineTerminal terminal, ClusterService clusterService, ClusterGroupService groupService, PlatformService platformService) {
        registerCommand(new ShutdownCommand(terminal, clusterService));
        registerCommand(new GroupCommand(groupService, platformService));
        registerCommand(new ClearCommand(terminal));
    }

    @Contract(pure = true)
    @Override
    public @Unmodifiable List<Command> commandsByName(String name) {
        return commands.stream().filter(it -> it.name().equalsIgnoreCase(name) || Arrays.stream(it.aliases()).anyMatch(s -> s.equalsIgnoreCase(name))).toList();
    }

    @Override
    public void registerCommand(Command command) {
        this.commands.add(command);
    }

    @Override
    public void unregisterCommand(Command command) {
        this.commands.remove(command);
    }

    @Override
    public void call(String commandId, String[] args) {
        CommandSerializer.serializer(this, commandId, args);
    }
}
