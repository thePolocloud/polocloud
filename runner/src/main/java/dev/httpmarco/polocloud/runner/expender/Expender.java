package dev.httpmarco.polocloud.runner.expender;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.*;

/**
 * Utility class to scan the current JAR for files under the ".cache" directory
 * and to read the manifests of embedded JAR files.
 */
public final class Expender {

    private static final Path CACHE_DIRECTORY = Paths.get(".cache");

    private Expender() {
        // Prevent instantiation
    }

    /**
     * Scans the current JAR for all files located under the ".cache" directory
     * and prints the manifests of all embedded JAR files in ".cache".
     *
     * @return a list of relative paths for all files in ".cache"
     */
    public static List<Path> scanJarCache() {
        try {
            Path jarPath = getOwnJarPath();

            // Print manifests for embedded JARs
            printEmbeddedJarManifests(jarPath);

            // Return all files under .cache
            return scanCacheFilesInJar(jarPath);

        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Prints the manifest of all JAR files located under ".cache" inside the main JAR.
     *
     * @param jarPath path to the main JAR file
     * @throws IOException if the JAR file cannot be read
     */
    private static void printEmbeddedJarManifests(Path jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (entry.getName().startsWith(CACHE_DIRECTORY + "/") && entry.getName().endsWith(".jar")) {
                    System.out.println("\nManifest of embedded JAR: " + entry.getName());

                    try (InputStream is = jarFile.getInputStream(entry);
                         JarInputStream jis = new JarInputStream(is)) {

                        Manifest manifest = jis.getManifest();
                        if (manifest != null) {
                            manifest.getMainAttributes().forEach((key, value) ->
                                    System.out.println(key + ": " + value)
                            );
                        } else {
                            System.out.println("No manifest found in " + entry.getName());
                        }
                    }
                }
            }
        }
    }

    /**
     * Reads the given JAR file and returns all files located under ".cache".
     *
     * @param jarPath the path to the JAR file
     * @return a list of relative paths for all files in ".cache"
     * @throws IOException if the JAR cannot be read
     */
    private static List<Path> scanCacheFilesInJar(Path jarPath) throws IOException {
        List<Path> cacheFiles = new ArrayList<>();

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (entry.getName().startsWith(CACHE_DIRECTORY + "/") && !entry.isDirectory()) {
                    cacheFiles.add(Paths.get(entry.getName()));
                }
            }
        }

        return cacheFiles;
    }

    /**
     * Determines the path to the current JAR file.
     *
     * @return the path to the current JAR
     * @throws URISyntaxException if the JAR URI is invalid
     */
    private static Path getOwnJarPath() throws URISyntaxException {
        URL jarUrl = Expender.class.getProtectionDomain().getCodeSource().getLocation();
        return Paths.get(jarUrl.toURI());
    }
}
