package dev.httpmarco.polocloud.node.launcher;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.httpmarco.polocloud.node.Node;
import dev.httpmarco.polocloud.node.NodeModule;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class NodeLauncher {
    public static void main(String[] args) {

        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            new RuntimeException(e);
        });


        try {
            Injector injector = Guice.createInjector(new NodeModule());
            System.setProperty("startup", String.valueOf(System.currentTimeMillis()));
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
