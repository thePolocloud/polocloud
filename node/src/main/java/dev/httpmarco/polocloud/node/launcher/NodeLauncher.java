package dev.httpmarco.polocloud.node.launcher;

import dev.httpmarco.polocloud.launcher.dependency.Dependency;
import dev.httpmarco.polocloud.launcher.dependency.DependencyDownloader;
import dev.httpmarco.polocloud.node.Node;

public class NodeLauncher {
    public static void main(String[] args) {

        // load logging dependencies
        DependencyDownloader.download(
                new Dependency("org.slf4j", "slf4j-api", "2.0.13"),
                new Dependency("org.apache.logging.log4j", "log4j-api", "2.23.1"),
                new Dependency("org.apache.logging.log4j", "log4j-core", "2.23.1"),
                new Dependency("org.apache.logging.log4j", "log4j-slf4j2-impl", "2.23.1"),
                new Dependency("org.jline", "jline", "3.26.3")
        );

        try {
            System.setProperty("startup", String.valueOf(System.currentTimeMillis()));
            new Node();
        } catch (Exception exception) {
            // check for remove
           exception.printStackTrace();
        }
    }
}
