package dev.httpmarco.polocloud.runner.utils;

import java.io.InputStream;
import java.util.jar.Manifest;

/**
 * Utility class for reading the application's own {@code MANIFEST.MF}.
 *
 * <p>This class provides helper methods to access metadata such as
 * version, build information, or implementation details from the
 * manifest file bundled with the running application.</p>
 */
public final class Manifests {

    /**
     * Private constructor to prevent instantiation.
     */
    private Manifests() {
        throw new UnsupportedOperationException("This is a utility class");
    }

    /**
     * Reads the {@code MANIFEST.MF} of the currently running application.
     *
     * <p>This method works both when running from an IDE and when
     * executed from a packaged JAR.</p>
     *
     * @return the parsed {@link Manifest} instance
     * @throws IllegalStateException if the manifest file cannot be found
     * @throws RuntimeException if an error occurs while reading the manifest
     */
    public static Manifest readOwnManifest() {
        try (InputStream is = Manifests.class
                .getClassLoader()
                .getResourceAsStream("META-INF/MANIFEST.MF")) {

            if (is == null) {
                throw new IllegalStateException("MANIFEST.MF not found on classpath");
            }

            return new Manifest(is);

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read MANIFEST.MF", e);
        }
    }
}
