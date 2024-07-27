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

    public @Nullable CommandExecution defaultExecution;
    public final List<SyntaxCommand> syntaxCommands = new ArrayList<>();

    public Command(String name, String... aliases) {
        this.name = name;
        this.aliases = aliases;
    }

    public void syntax(CommandExecution execution, CommandArgument<?>... arguments) {
        this.syntaxCommands.add(new SyntaxCommand(execution, arguments, null));
    }

    public void syntax(CommandExecution execution, String description, CommandArgument<?>... arguments) {
        this.syntaxCommands.add(new SyntaxCommand(execution, arguments, description));
    }

    public void defaultExecution(CommandExecution execution) {
        this.defaultExecution = execution;
    }

    public boolean hasSyntaxCommands() {
        return !this.syntaxCommands.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Command command && command.name.equals(name);
    }
}
