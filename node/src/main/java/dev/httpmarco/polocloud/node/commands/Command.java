package dev.httpmarco.polocloud.node.commands;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Accessors(fluent = true)
public abstract class Command {

    private final String name;
    private final String[] aliases;
    private final String description;

    private @Nullable CommandExecution defaultExecution;
    private final List<CommandSyntax> commandSyntaxes = new ArrayList<>();

    public Command(String name, String description, String... aliases) {
        this.name = name;
        this.description = description;
        this.aliases = aliases;
    }

    public void syntax(CommandExecution execution, CommandArgument<?>... arguments) {
        this.commandSyntaxes.add(new CommandSyntax(execution, arguments, null));
    }

    public void syntax(CommandExecution execution, String description, CommandArgument<?>... arguments) {
        this.commandSyntaxes.add(new CommandSyntax(execution, arguments, description));
    }

    public void defaultExecution(CommandExecution execution) {
        this.defaultExecution = execution;
    }

    public boolean hasSyntaxCommands() {
        return !this.commandSyntaxes.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Command command && command.name.equals(name);
    }
}
