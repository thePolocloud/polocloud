package de.polocloud.runner;

import de.polocloud.runner.utils.FileHelper;
import de.polocloud.runner.utils.Manifests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.jar.Manifest;

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
     * System property that defines the startup time of polocloud.
     */
    public static final String STARTUP_TIME = "polocloud.startup";

    /**
     * Give the own jar path to the thread
     * We attach the node/cli jar into the classpath and by default we collect the runner path
     */
    public static final String RUNTIME_PATH = "polocloud.runtime.path";

    /**
     * Give the common jar path to the thread
     * We attach the common jar into the classpath
     */
    public static final String COMMON_PATH = "polocloud.common.path";

    /**
     * Give the common jar path to the thread
     * We attach the common jar into the classpath
     */
    public static final String PROTO_PATH = "polocloud.proto.path";

    /**
     * Give the common jar path to the thread
     * We attach the common jar into the classpath
     */
    public static final String SHARED_PATH = "polocloud.shared.path";


    /**
     * System property the launcher stores its original process arguments under, joined
     * with {@link #LAUNCH_ARGS_SEPARATOR}. Lets a self-update ({@code de.polocloud.updater.Updater})
     * relaunch this same jar with the same arguments after replacing it on disk.
     */
    public static final String LAUNCH_ARGS = "polocloud.launch.args";

    /** Separator {@link #LAUNCH_ARGS} is joined with — arbitrary args never contain this control character. */
    public static final String LAUNCH_ARGS_SEPARATOR = "";

    /**
     * System property that defines the join token, to join in a cluster.
     */
    public static final String JOIN_TOKEN = "polocloud.join.token";

    /**
     * System property that defines the join host, to join in a cluster.
     */
    public static final String JOIN_HOST  = "polocloud.join.host";

    /**
     * System property that defines the join port, to join in a cluster.
     */
    public static final String JOIN_PORT  = "polocloud.join.port";

    /**
     * System property that defines the group name.
     *
     * <p>Example value: {@code node-eu}</p>
     */
    public static final String NODE_GROUP = "polocloud.node.group";

    /**
     * Name of the initialization control file used by Polocloud to track the initialization state of the CLI or node.
     *
     * <p>This file is stored in the "local" directory and contains metadata about the initialization process,</p>
     *  <p>such as timestamps, status, and any relevant information needed to determine if the CLI or node has been initialized.</p>
     */
    public static final String INITIALIZATION_CONTROL_FILE_NAME = "local/initialization.info";

    /**
     * Path to the runtime cache folder used by Polocloud for temporary and cached files.
     *
     * <p>This folder is created in the current working directory.</p>
     */
    public static final Path EXPENDER_RUNTIME_CACHE = Paths.get(".cache/dependencies");

    /** Manifest attribute key for the Kotlin runtime version. */
    private static final String MANIFEST_KOTLIN_VERSION = "kotlin-version";

    /** Manifest attribute key for the Log4j version. */
    private static final String MANIFEST_LOG4J_VERSION  = "log4j-version";

    /** Manifest attribute key for the SLF4J version. */
    private static final String MANIFEST_SLF4J_VERSION  = "slf4j-version";

    /**
     * Cached manifest instance of the currently running application.
     */
    private static final Manifest MANIFEST = Manifests.readOwnManifest();

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private PolocloudParameters() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Reads a required attribute from the application manifest.
     *
     * @param key the manifest attribute key
     * @return the resolved value
     * @throws IllegalStateException if the attribute is missing
     */
    private static String requireManifestValue(String key) {
        String value = MANIFEST.getMainAttributes().getValue(key);

        if (value == null) {
            throw new IllegalStateException("Missing '" + key + "' in MANIFEST.MF");
        }

        return value;
    }

    /** Resolves the Kotlin runtime version from the manifest. */
    public static String kotlinVersion() {
        return requireManifestValue(MANIFEST_KOTLIN_VERSION);
    }

    /** Resolves the Log4j version from the manifest. */
    public static String log4jVersion() {
        return requireManifestValue(MANIFEST_LOG4J_VERSION);
    }

    /** Resolves the SLF4J version from the manifest. */
    public static String slf4jVersion() {
        return requireManifestValue(MANIFEST_SLF4J_VERSION);
    }

    /** Maven Central URL for kotlin-stdlib. */
    public static String kotlinDownloadUrl() {
        String v = kotlinVersion();
        return "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/"
                + v + "/kotlin-stdlib-" + v + ".jar";
    }

    /** Local cache path of the Kotlin runtime JAR. */
    public static Path bootKotlin() {
        String v = kotlinVersion();
        return EXPENDER_RUNTIME_CACHE.resolve(Paths.get(
                "org", "jetbrains", "kotlin", "kotlin-stdlib",
                v, "kotlin-stdlib-" + v + ".jar"
        ));
    }

    /** Maven Central URL for log4j-api. */
    public static String log4jApiDownloadUrl() {
        String v = log4jVersion();
        return "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-api/"
                + v + "/log4j-api-" + v + ".jar";
    }

    /** Maven Central URL for log4j-core. */
    public static String log4jCoreDownloadUrl() {
        String v = log4jVersion();
        return "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/"
                + v + "/log4j-core-" + v + ".jar";
    }

    /** Maven Central URL for log4j-slf4j2-impl. */
    public static String log4jSlf4jImplDownloadUrl() {
        String v = log4jVersion();
        return "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-slf4j2-impl/"
                + v + "/log4j-slf4j2-impl-" + v + ".jar";
    }

    /** Maven Central URL for slf4j-api. */
    public static String slf4jApiDownloadUrl() {
        String v = slf4jVersion();
        return "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/"
                + v + "/slf4j-api-" + v + ".jar";
    }

    /** Local cache path for log4j-api JAR (mirrors Dependency.localPath() structure). */
    public static Path bootLog4jApi() {
        String v = log4jVersion();
        return EXPENDER_RUNTIME_CACHE.resolve(Paths.get(
                "org", "apache", "logging", "log4j", "log4j-api",
                v, "log4j-api-" + v + ".jar"
        ));
    }

    /** Local cache path for log4j-core JAR. */
    public static Path bootLog4jCore() {
        String v = log4jVersion();
        return EXPENDER_RUNTIME_CACHE.resolve(Paths.get(
                "org", "apache", "logging", "log4j", "log4j-core",
                v, "log4j-core-" + v + ".jar"
        ));
    }

    /** Local cache path for log4j-slf4j2-impl JAR. */
    public static Path bootLog4jSlf4jImpl() {
        String v = log4jVersion();
        return EXPENDER_RUNTIME_CACHE.resolve(Paths.get(
                "org", "apache", "logging", "log4j", "log4j-slf4j2-impl",
                v, "log4j-slf4j2-impl-" + v + ".jar"
        ));
    }

    /** Local cache path for slf4j-api JAR. */
    public static Path bootSlf4jApi() {
        String v = slf4jVersion();
        return EXPENDER_RUNTIME_CACHE.resolve(Paths.get(
                "org", "slf4j", "slf4j-api",
                v, "slf4j-api-" + v + ".jar"
        ));
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
     * <p>Example: {@code .cache/dependencies/dev/httpmarco/polocloud/cli/1.0.0/cli-1.0.0.jar}</p>
     */
    public static Path expenderRuntimeCache(String project) {
        return EXPENDER_RUNTIME_CACHE.resolve(Paths.get(
                "de",
                "polocloud",
                project,
                version(),
                project + "-" + version() + ".jar"
        ));
    }

    /**
     * Ensures the runtime cache directory exists and is marked as hidden on Windows.
     *
     * <p>This method must be called once at launcher startup, before any other component
     * attempts to write into the cache directory.</p>
     *
     * @throws RuntimeException if the cache directory cannot be created
     */
    public static void ensureCacheDirectory() {
        try {
            Files.createDirectories(EXPENDER_RUNTIME_CACHE);
            FileHelper.hideFile(EXPENDER_RUNTIME_CACHE.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize cache", e);
        }
    }
}
