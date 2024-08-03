package dev.httpmarco.polocloud.node.commands;

import java.util.List;

public interface CommandService {

    List<Command> commands();

    List<Command> commandsByName(String name);

    void registerCommand(Command command);

    void registerCommands(Command... command);

    void unregisterCommand(Command command);

    void call(String commandId, String[] args);

}
