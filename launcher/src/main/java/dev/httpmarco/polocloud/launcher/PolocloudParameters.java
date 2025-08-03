package dev.httpmarco.polocloud.launcher;

import java.io.IOException;
import java.nio.file.Path;

public final class PolocloudParameters {

    /**
     * General class for all parameters
     * For type-safe use
     */
    public static String VERSION_ENV_ID = "polocloud-version";
    public static Path LIB_DIRECTORY = Path.of("local/libs");
    public static Path DEPENDENCY_DIRECTORY = Path.of("local/dependencies");
    public static String[] REQUIRED_LIBS = {"proto", "agent", "common", "platforms", "bridge-velocity",  "bridge-bungeecord"};
    public static String BOOT_LIB = "agent";

    /**
     * Reads a specific key from the manifest of a JAR file located at the given path.
     *
     * @param key The key to read from the manifest.
     * @param jarPath The path to the JAR file.
     * @return The value associated with the key in the manifest, or null if not found or an error occurs.
     */
    public static String readManifest(String key, Path jarPath) {
        try (var jarFile = new java.util.jar.JarFile(jarPath.toFile())) {
            var attributes = jarFile.getManifest().getMainAttributes();
            return attributes.getValue(key);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    public static String polocloudVersion() {
       return System.getProperty(VERSION_ENV_ID);
    }
}
