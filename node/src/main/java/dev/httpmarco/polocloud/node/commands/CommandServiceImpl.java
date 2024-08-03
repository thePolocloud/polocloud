package dev.httpmarco.polocloud.node.commands;

import dev.httpmarco.polocloud.node.terminal.commands.*;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Accessors(fluent = true)
public final class CommandServiceImpl implements CommandService {

    private final List<Command> commands = new ArrayList<>();

    public CommandServiceImpl() {
        this.registerCommand(new ShutdownCommand());
        this.registerCommand(new ClearCommand());
        this.registerCommand(new ReloadCommand());
        this.registerCommand(new HelpCommand());
        this.registerCommand(new InfoCommand());
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
    public void registerCommands(Command @NotNull ... commands) {
        for (var command : commands) {
            registerCommand(command);
        }
    }


    @Override
    public void unregisterCommand(Command command) {
        this.commands.remove(command);
    }

    @Override
    public void call(String commandId, String[] args) {
        CommandParser.serializer(this, commandId, args);
    }
}
