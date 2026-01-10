package dev.httpmarco.polocloud.runner;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Holds constant parameters, paths, and environment variable names
 * used by the Polocloud runner.
 *
 * <p>This class is a utility class and is not intended to be instantiated.</p>
 */
public final class PolocloudParameters {

    /**
     * Environment variable or system property that defines the Polocloud version
     * used by the runner.
     *
     * <p>Example value: {@code 1.0.0}</p>
     */
    public static final String VERSION_ENV = "version";

    /**
     * Path to the runtime cache folder used by Polocloud for temporary and cached files.
     *
     * <p>This folder is created in the current working directory.</p>
     */
    public static final Path EXPENDER_RUNTIME_CACHE = Paths.get(".cache");

    /**
     * Version of the Kotlin runtime used by Polocloud.
     */
    public static final String KOTLIN_VERSION = "2.3.0";

    /**
     * URL from which the Kotlin runtime JAR can be downloaded.
     *
     * <p>Example:
     * {@code https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/2.3.0/kotlin-stdlib-2.3.0.jar}</p>
     */
    public static final String KOTLIN_DOWNLOAD_URL =
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/"
                    + KOTLIN_VERSION
                    + "/kotlin-stdlib-"
                    + KOTLIN_VERSION
                    + ".jar";

    /**
     * Path to the Kotlin runtime JAR used by Polocloud.
     *
     * <p>The path is resolved relative to {@link #EXPENDER_RUNTIME_CACHE}.</p>
     *
     * <p>Example: {@code .cache/org/jetbrains/kotlin/kotlin-stdlib/2.3.0/kotlin-stdlib-2.3.0.jar}</p>
     */
    public static final Path BOOT_KOTLIN = EXPENDER_RUNTIME_CACHE.resolve(Paths.get(
            "org",
            "jetbrains",
            "kotlin",
            "kotlin-stdlib",
            KOTLIN_VERSION,
            "kotlin-stdlib-" + KOTLIN_VERSION + ".jar"
    ));

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private PolocloudParameters() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Retrieves the Polocloud version from the system property or environment variable.
     *
     * @return the Polocloud version, or {@code null} if not set
     */
    public static String version() {
        return System.getProperty(VERSION_ENV);
    }

    /**
     * Path to the Polocloud CLI boot JAR file.
     *
     * <p>The path is resolved relative to {@link #EXPENDER_RUNTIME_CACHE} and depends
     * on the Polocloud version returned by {@link #version()}.</p>
     *
     * <p>Example: {@code .cache/dev/httpmarco/polocloud/cli/1.0.0/cli-1.0.0.jar}</p>
     */
    public static Path expenderRuntimeCache(String project) {
        return EXPENDER_RUNTIME_CACHE.resolve(Paths.get(
                "dev",
                "httpmarco",
                "polocloud",
                project,
                version(),
                project + "-" + version() + ".jar"
        ));
    }
}
