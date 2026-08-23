package de.polocloud.runner.expender;

import de.polocloud.runner.PolocloudParameters;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Downloads (once) and caches the bootstrap libraries - kotlin, log4j, slf4j - that must be
 * on the classpath before any Polocloud module can load.
 *
 * <p>Shared by the normal boot path ({@link de.polocloud.runner.runtime.AbstractRuntimeProcess})
 * and {@link de.polocloud.runner.runtime.impl.PrimeCacheRuntimeProcess}, which downloads them
 * ahead of time - e.g. as a step in a Docker image build - so a container never needs to reach
 * Maven Central at runtime. Once a jar exists at its target path it is never re-downloaded.</p>
 */
public final class BootstrapLibraries {

    // Mirrors updater's Updater#downloadToTemp / ReleaseFetcher timeouts, so a stalled
    // Maven Central connection can't hang node boot indefinitely.
    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 30_000;

    private BootstrapLibraries() {}

    public static void ensurePresent() throws IOException, URISyntaxException {
        ensureJar(PolocloudParameters.bootKotlin(),        PolocloudParameters.kotlinDownloadUrl());
        ensureJar(PolocloudParameters.bootLog4jApi(),      PolocloudParameters.log4jApiDownloadUrl());
        ensureJar(PolocloudParameters.bootLog4jCore(),     PolocloudParameters.log4jCoreDownloadUrl());
        ensureJar(PolocloudParameters.bootLog4jSlf4jImpl(), PolocloudParameters.log4jSlf4jImplDownloadUrl());
        ensureJar(PolocloudParameters.bootSlf4jApi(),      PolocloudParameters.slf4jApiDownloadUrl());
    }

    private static void ensureJar(Path target, String downloadUrl) throws IOException, URISyntaxException {
        if (Files.exists(target)) {
            return;
        }

        System.out.println("[Bootstrap] Downloading " + target.getFileName() + " ...");
        Files.createDirectories(target.getParent());

        URLConnection connection = new URI(downloadUrl).toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);

        try (InputStream inputStream = connection.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("[Bootstrap] Downloaded  " + target.getFileName());
    }
}
