package dev.httpmarco.polocloud.runner;

import dev.httpmarco.polocloud.runner.expender.ExpenderRuntimeCache;

public final class PolocloudProcess {

    public int start() {
        ExpenderRuntimeCache.migrateCacheFiles();

        ProcessBuilder processBuilder = new ProcessBuilder()
                .command("java", "-jar", PolocloudParameters.BOOT_CLI.toString())
                .inheritIO();

        try {
            Process process = processBuilder.start();
            return process.waitFor();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            e.printStackTrace(System.err);
            return 1;
        }
    }
}
