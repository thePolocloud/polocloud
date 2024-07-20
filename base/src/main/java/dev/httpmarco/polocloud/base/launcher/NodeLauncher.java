package dev.httpmarco.polocloud.base.launcher;

import dev.httpmarco.polocloud.base.Node;
import dev.httpmarco.polocloud.base.NodeShutdownTask;

public final class NodeLauncher {

    public static void main(String[] args) {
        System.setProperty("startup", String.valueOf(System.currentTimeMillis()));

        Runtime.getRuntime().addShutdownHook(new Thread(NodeShutdownTask::run));

        new Node();
    }
}
