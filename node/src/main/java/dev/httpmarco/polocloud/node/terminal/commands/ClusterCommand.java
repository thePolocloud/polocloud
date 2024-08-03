package dev.httpmarco.polocloud.node.terminal.commands;

import dev.httpmarco.polocloud.node.cluster.ClusterService;
import dev.httpmarco.polocloud.node.cluster.NodeEndpoint;
import dev.httpmarco.polocloud.node.commands.Command;
import dev.httpmarco.polocloud.node.commands.CommandArgumentType;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ClusterCommand extends Command {

    public ClusterCommand(ClusterService clusterService) {
        super("cluster", "Manager your cluster");

        var endpoints = clusterService.endpoints();

        var nodeArgument = CommandArgumentType.NodeEndpoint(clusterService, "node");

        syntax(context -> {
            if (endpoints.isEmpty()) {
                log.info("There are no endpoints registered&!");
                return;
            }

            log.info("Following &f{} &7endpoints are registered&8:", endpoints.size());
            for (NodeEndpoint endpoint : endpoints) {
                log.info("&8- &f{}&8: (&7{}&8)", endpoint.data().name(), endpoint.data().details());
            }
        }, "List all Node endpoints&8.", CommandArgumentType.Keyword("list"));

        syntax(context -> {
            var node = context.arg(nodeArgument);
            log.info("Name&8: &b{}", node.data().name());
            log.info("Hostname&8: &b{}", node.data().hostname());
            log.info("Port&8: &b{}", node.data().port());
            log.info("Situation&8: &b{}", node.situation());
        }, "Show all information about a node&8.", nodeArgument, CommandArgumentType.Keyword("info"));

    }
}
