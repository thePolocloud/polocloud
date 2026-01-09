package dev.httpmarco.polocloud.runner;

import dev.httpmarco.polocloud.runner.expender.ExpenderRuntimeCache;

/**
 * Represents a Polocloud process that can be started via the CLI.
 * <p>
 * This class handles the migration of runtime cache files before starting
 * a new Java process with the specified CLI JAR.
 * </p>
 * Usage example:
 * <pre>{@code
 * PolocloudProcess process = new PolocloudProcess();
 * int exitCode = process.start();
 * }</pre>
 */
public final class PolocloudProcess {

    /**
     * Starts the Polocloud process.
     * <p>
     * This method performs the following steps:
     * <ol>
     *     <li>Migrates runtime cache files using {@link ExpenderRuntimeCache#migrateCacheFiles()}.</li>
     *     <li>Builds and starts a new Java process running the CLI JAR defined in {@link PolocloudParameters#BOOT_CLI}.</li>
     *     <li>Waits for the process to finish and returns its exit code.</li>
     * </ol>
     *
     * @return the exit code of the started process; returns 1 if an exception occurs
     */
    public int start() {
        // Migrate cache files before starting the process
        ExpenderRuntimeCache.migrateCacheFiles();

        ProcessBuilder processBuilder = new ProcessBuilder()
                .command("java", "-jar", PolocloudParameters.BOOT_CLI.toString())
                .inheritIO(); // Output of the process is redirected to the current console

        try {
            Process process = processBuilder.start();
            return process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            System.err.println("Process was interrupted:");
            e.printStackTrace(System.err);
            return 1;
        } catch (Exception e) {
            System.err.println("Failed to start Polocloud process:");
            e.printStackTrace(System.err);
            return 1;
        }
    }
}
