package dev.httpmarco.polocloud.node.launcher;

import dev.httpmarco.polocloud.launcher.dependency.Dependency;
import dev.httpmarco.polocloud.launcher.dependency.DependencyDownloader;
import dev.httpmarco.polocloud.node.Node;

public class NodeLauncher {
    public static void main(String[] args) {

        var log4jCoreDependency = new Dependency("org.apache.logging.log4j", "log4j-core", "2.23.1", true);
        var log4jSlf4jDependency = new Dependency("org.apache.logging.log4j", "log4j-slf4j2-impl", "2.23.1");
        var jlineDependency = new Dependency("org.jline", "jline", "3.26.3");

        // load logging dependencies
        DependencyDownloader.download(log4jCoreDependency, log4jSlf4jDependency, jlineDependency);

        try {
            System.setProperty("startup", String.valueOf(System.currentTimeMillis()));
            new Node();
        } catch (Exception exception) {
            // check for remove
           exception.printStackTrace();
        }
    }
}
