package dev.httpmarco.polocloud.node.launcher;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.httpmarco.polocloud.launcher.dependency.Dependency;
import dev.httpmarco.polocloud.launcher.dependency.DependencyDownloader;
import dev.httpmarco.polocloud.node.Node;
import dev.httpmarco.polocloud.node.NodeModule;

public class NodeLauncher {
    public static void main(String[] args) {

        // load logging dependencies
        DependencyDownloader.download(
                new Dependency("org.apache.logging.log4j", "log4j-core", "2.23.1"),
                new Dependency("org.apache.logging.log4j", "log4j-slf4j2-impl", "2.23.1"),
                new Dependency("org.jline", "jline", "3.26.3")
        );


        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            new RuntimeException(e);
        });


        try {
            System.setProperty("startup", String.valueOf(System.currentTimeMillis()));
            Injector injector = Guice.createInjector(new NodeModule());
            injector.getInstance(Node.class);
        } catch (Exception exception) {
            for (var errorLine : exception.getMessage().split("\\n", -1)) {
                System.err.println(errorLine);
            }

            for (StackTraceElement traceElement : exception.getStackTrace()) {
                System.err.println(traceElement);
            }

            for (StackTraceElement throwable : exception.getCause().getStackTrace()) {
                System.err.println(throwable);
            }
        }
    }
}
