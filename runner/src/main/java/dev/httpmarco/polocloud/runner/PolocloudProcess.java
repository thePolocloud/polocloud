package dev.httpmarco.polocloud.runner;

import dev.httpmarco.polocloud.runner.expender.ExpenderRuntimeCache;

import java.io.IOException;
import java.nio.file.Path;

public final class PolocloudProcess {

    private static final Path bootJar = Path.of(".cache");
    private Process process;

    public void start(String runtimeVersion) {
        ExpenderRuntimeCache.migrateCacheFiles();

        ProcessBuilder processBuilder = new ProcessBuilder()
                .command("java", "-jar", bootJar
                        .resolve("dev")
                        .resolve("httpmarco")
                        .resolve("polocloud")
                        .resolve("cli")
                        .resolve(runtimeVersion)
                        .resolve("cli-" + runtimeVersion + ".jar").toString())
                .inheritIO();

        processBuilder.environment().putAll(System.getenv());

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
