package dev.httpmarco.polocloud.node.terminal.commands;

import dev.httpmarco.polocloud.node.Node;
import dev.httpmarco.polocloud.node.commands.Command;
import dev.httpmarco.polocloud.node.packets.ClusterReloadCallPacket;
import dev.httpmarco.polocloud.node.packets.ClusterRequireReloadPacket;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class ReloadCommand extends Command {

    public ReloadCommand() {
        super("reload", "rl");

        var clusterService = Node.instance().clusterService();

        defaultExecution((it) -> {
            log.info("Start reloading of cluster&8...");

            if (clusterService.localHead()) {
                clusterService.broadcastAll(new ClusterReloadCallPacket());
            } else {
                // call head node for reloading all cluster points
                clusterService.headNode().transmit().sendPacket(new ClusterRequireReloadPacket());
            }


            log.info("Successfully reloading&8!");
        });
    }
}
