package dev.httpmarco.polocloud.runner;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Holds constant parameter and environment variable names
 * used by the Polocloud runner.
 *
 * <p>This class is not intended to be instantiated.</p>
 */
public final class PolocloudParameters {

    /**
     * Environment variable that defines the Polocloud version
     * used by the runner.
     *
     * <p>Example value: {@code 1.0.0}</p>
     */
    public static final String VERSION_ENV = "version";

    /**
     * Environment variable that defines the cache folder
     * used by the runner.
     */
    public static final Path EXPENDER_RUNTIME_CACHE = Paths.get(".cache");

    /**
     * Environment variable that defines the boot jar
     * used by the runner.
     */
    public static final Path BOOT_CLI = EXPENDER_RUNTIME_CACHE.resolve(Paths.get(
            "dev",
            "httpmarco",
            "polocloud",
            "cli",
            version(),
            "/cli-" + version() + ".jar"
    ));

    /**
     * Private constructor to prevent instantiation.
     */
    private PolocloudParameters() {
        throw new UnsupportedOperationException("This is a utility class");
    }

    public static String version() {
        return System.getProperty(VERSION_ENV);
    }
}
