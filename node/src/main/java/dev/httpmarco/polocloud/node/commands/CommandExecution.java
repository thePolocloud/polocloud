package dev.httpmarco.polocloud.node.commands;

import dev.httpmarco.polocloud.node.commands.CommandContext;

public interface CommandExecution {

    void execute(CommandContext commandContext);

}
