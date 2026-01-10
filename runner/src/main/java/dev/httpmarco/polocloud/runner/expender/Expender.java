package dev.httpmarco.polocloud.runner.expender;

import dev.httpmarco.polocloud.runner.PolocloudParameters;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

/**
 * Utility class to scan the current JAR for files under the ".cache" directory
 * and to read the manifests of embedded JAR files.
 */
public final class Expender {

    private Expender() {
        // Prevent instantiation
    }

    /**
     * Scans the current JAR for all files located under the ".cache" directory
     * and returns them as ExpenderElements with artifactId, groupId, version, and file.
     *
     * @return a list of ExpenderElements representing each embedded JAR in ".cache"
     */
    public static List<ExpenderElements> scanJarCache() {
        List<ExpenderElements> elements = new ArrayList<>();
        try {
            Path jarPath = getOwnJarPath();
            elements.addAll(scanCacheElementsInJar(jarPath));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace(System.err);
        }
        return elements;
    }

    /**
     * Reads the given JAR file and returns all embedded JAR files under ".cache" as ExpenderElements.
     *
     * @param jarPath the path to the main JAR
     * @return list of ExpenderElements
     * @throws IOException if reading the JAR fails
     */
    private static List<ExpenderElements> scanCacheElementsInJar(Path jarPath) throws IOException {
        List<ExpenderElements> elements = new ArrayList<>();

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (entry.getName().startsWith(PolocloudParameters.EXPENDER_RUNTIME_CACHE + "/") && entry.getName().endsWith(".jar")) {
                    File tempFile = Files.createTempFile("expender-", ".jar").toFile();
                    tempFile.deleteOnExit();

                    try (InputStream is = jarFile.getInputStream(entry);
                         OutputStream os = Files.newOutputStream(tempFile.toPath())) {

                        byte[] buffer = new byte[8192];
                        int bytesRead;

                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }

                    try (JarInputStream jis = new JarInputStream(new FileInputStream(tempFile))) {
                        Manifest manifest = jis.getManifest();
                        if (manifest != null) {
                            Attributes attrs = manifest.getMainAttributes();

                            String artifactId = attrs.getValue("artifactId");
                            String groupId = attrs.getValue("groupId");
                            String version = attrs.getValue(PolocloudParameters.VERSION_ENV);
                            String mainClass = attrs.getValue("Main-Class");

                            elements.add(new ExpenderElements(artifactId, groupId, version, tempFile, mainClass));
                        }

                    }
                }
            }
        }

        return elements;
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
