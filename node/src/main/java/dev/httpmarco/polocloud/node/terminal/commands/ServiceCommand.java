package dev.httpmarco.polocloud.node.terminal.commands;

import dev.httpmarco.polocloud.node.Node;
import dev.httpmarco.polocloud.node.commands.*;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class ServiceCommand extends Command {

    public ServiceCommand() {
        super("service", "services");

        syntax(commandContext -> {
            var serviceProvider = Node.instance().serviceProvider();

            log.info("Following &b{} &7groups are loading&8:", serviceProvider.services().size());
            serviceProvider.services().forEach(group -> log.info("&8- &f{}&8: (&7{}&8)", group.name(), group.details()));
        }, CommandArgumentType.Keyword("list"));
    }
}
