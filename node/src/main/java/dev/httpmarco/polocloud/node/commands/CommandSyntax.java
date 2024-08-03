package dev.httpmarco.polocloud.node.commands;

import dev.httpmarco.polocloud.node.commands.type.KeywordArgument;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public final class CommandSyntax {

    private final CommandExecution execution;
    private final CommandArgument<?>[] arguments;
    private @Nullable String description;

    public @NotNull String usage() {
        return String.join(" ", Arrays.stream(arguments)
                .map(it -> it instanceof KeywordArgument ? "&f" + it.key() : "&8<&f" + it.key() + "&8>")
                .toList()) +
                (description == null ? "" : " &8- &7" + description);
    }
}
