package dev.httpmarco.polocloud.node.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class SyntaxCommand {

    private final CommandExecution execution;
    private final CommandArgument<?>[] arguments;
    private @Nullable String description;

    public String usage() {
        return String.join(" ", Arrays.stream(arguments).map(it -> "&8<&f" + it.key() + "&8>").toList()) + (description == null ? "" : " &8- &7" + description);
    }
}
