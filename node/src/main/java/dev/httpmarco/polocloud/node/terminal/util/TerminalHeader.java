package dev.httpmarco.polocloud.node.terminal.util;

import dev.httpmarco.polocloud.node.NodeConfig;
import dev.httpmarco.polocloud.node.terminal.JLineTerminal;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TerminalHeader {

    public void print(JLineTerminal terminal, NodeConfig localNode) {
        terminal.printLine("");
        terminal.printLine("   &fPoloCloud &8- &7Simple minecraft cloudsystem &8- &7v1.0.10-snapshot");
        terminal.printLine("   &7Local node&8: &7" + localNode.localNode().name() + " &8| &7External nodes&8: &8[]");
        terminal.printLine("");
    }

}
