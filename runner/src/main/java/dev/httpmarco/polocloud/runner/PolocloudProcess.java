package dev.httpmarco.polocloud.runner;

import dev.httpmarco.polocloud.runner.expender.ExpenderRuntimeCache;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Represents a Polocloud process that can be started via the CLI.
 * <p>
 * Handles runtime cache migration and ensures the Kotlin runtime
 * is available before starting the CLI process.
 */
public final class PolocloudProcess {

    /**
     * Starts the Polocloud process.
     *
     * @return the exit code of the started process, or 1 on failure
     */
    public int start() {
        try {
            ExpenderRuntimeCache.migrateCacheFiles();
            prepareKotlinRuntime();

            Process process = createProcessBuilder().start();
            return process.waitFor();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Process execution was interrupted");
        } catch (Exception e) {
            System.err.println("Failed to start Polocloud process");
            e.printStackTrace(System.err);
        }

        return 1;
    }

    private ProcessBuilder createProcessBuilder() {
        String classpath = PolocloudParameters.BOOT_KOTLIN
                + java.io.File.pathSeparator
                + PolocloudParameters.expenderRuntimeCache("cli")
                + java.io.File.pathSeparator
                + PolocloudParameters.expenderRuntimeCache("common");

        return new ProcessBuilder(
                "java",
                "-cp",
                classpath,
                Objects.requireNonNull(ExpenderRuntimeCache.findElementByArtifactId("cli")).mainClass()
        ).inheritIO();
    }

    private void prepareKotlinRuntime() throws IOException {
        Path kotlinJar = PolocloudParameters.BOOT_KOTLIN;

        if (Files.exists(kotlinJar)) {
            return;
        }

        Files.createDirectories(kotlinJar.getParent());
        try (InputStream in = new URL(PolocloudParameters.KOTLIN_DOWNLOAD_URL).openStream()) {
            Files.copy(in, kotlinJar, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
