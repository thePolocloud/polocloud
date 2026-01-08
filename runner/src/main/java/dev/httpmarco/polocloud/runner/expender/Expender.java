package dev.httpmarco.polocloud.runner.expender;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Expender {

    /**
     * Scans the current JAR file for all files located under the ".cache" directory.
     *
     * @return a list of relative paths for all files in ".cache"
     */
    public static List<Path> scanJarCache() {
        try {
            Path jarPath = getOwnJarPath();
            return scanJarCache(jarPath);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Reads the given JAR file and returns all files located under ".cache".
     *
     * @param jarPath the path to the JAR file
     * @return a list of relative paths for all files in ".cache"
     * @throws IOException if the JAR file cannot be read
     */
    private static List<Path> scanJarCache(Path jarPath) throws IOException {
        List<Path> cacheFiles = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith(".cache/") && !entry.isDirectory()) {
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
