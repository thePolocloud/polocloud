package dev.httpmarco.polocloud.runner;

import java.io.IOException;
import java.nio.file.Path;

public final class PolocloudProcess {

    private static final Path runtimeCacheDirectory = Path.of(System.getProperty("user.dir"), ".cache", "libs", "");
    private Process process;

    public void start() {
        ProcessBuilder processBuilder = new ProcessBuilder()
                .command("java", "-jar", "polocloud-server.jar")
                .inheritIO();

        try {
            this.process = processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    public int waitForExit() {
        try {
            return this.process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace(System.err);
            return -1;
        }
    }
}
