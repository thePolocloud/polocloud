package dev.httpmarco.polocloud.node.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public abstract class CommandArgument<T> {

    private String key;

    public List<String> defaultArgs() {
        return List.of();
    }

    // if one argument must be special type
    public boolean predication(@NotNull String rawInput) {
        return !(rawInput.startsWith("<") && rawInput.endsWith(">"));
    }

    public String wrongReason() {
        return "The argument " + key + " is not a valid parameter&8!";
    }

    public abstract T buildResult(String input);
}
