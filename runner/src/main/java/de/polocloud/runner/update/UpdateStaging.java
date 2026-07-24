package de.polocloud.runner.update;

import de.polocloud.runner.PolocloudParameters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Applies an update staged by {@code de.polocloud.updater.Updater} (the `updater`
 * module), at the very start of boot before {@link de.polocloud.runner.runtime.AbstractRuntimeProcess}
 * builds its classpath.
 *
 * <p>The updater deliberately never touches the launcher jar itself (see its class
 * doc) — it only ever unpacks a newer release's module jars under {@link #UPDATE_DIR}.
 * Applying that update here means moving those jars into {@link PolocloudParameters#EXPENDER_RUNTIME_CACHE}
 * (the same layout {@code ExpenderRuntimeCache} already uses) and pointing {@link PolocloudParameters#VERSION_ENV}
 * at the new version, so every module path resolved during this boot points at the
 * staged jars instead of the ones still embedded in this (unchanged) launcher jar.</p>
 *
 * <p>Assumes each module's main class name is stable across versions: the launcher's
 * own {@code ExpenderRuntimeCache} still resolves entry points (main class names) from
 * what's embedded in this jar, not from the newly staged ones, since scanning inside
 * a jar we may just have staged over the version resolution below would only pay off
 * if a module's main class actually changed.</p>
 */
public final class UpdateStaging {

    private static final Path UPDATE_DIR = Paths.get(".cache", "update");
    private static final String VERSION_MARKER_FILE = "version";

    private UpdateStaging() {}

    /** No-op if nothing is staged. Never throws: a failed apply just keeps the current version running. */
    public static void applyIfPresent() {
        Path marker = UPDATE_DIR.resolve(VERSION_MARKER_FILE);
        if (!Files.isRegularFile(marker)) {
            return;
        }

        try {
            String newVersion = new String(Files.readAllBytes(marker), StandardCharsets.UTF_8).trim();
            migrateStagedJars();
            System.setProperty(PolocloudParameters.VERSION_ENV, newVersion);
            System.out.println("[Update] Applied staged update, now running " + newVersion);
        } catch (IOException e) {
            System.err.println("[Update] Failed to apply staged update, keeping the current version: " + e.getMessage());
        } finally {
            deleteQuietly(UPDATE_DIR);
        }
    }

    private static void migrateStagedJars() throws IOException {
        try (Stream<Path> stream = Files.walk(UPDATE_DIR)) {
            List<Path> jars = stream.filter(p -> p.toString().endsWith(".jar")).collect(Collectors.toList());
            for (Path jar : jars) {
                Path relative = UPDATE_DIR.relativize(jar);
                Path target = PolocloudParameters.EXPENDER_RUNTIME_CACHE.resolve(relative);
                Files.createDirectories(target.getParent());
                Files.move(jar, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void deleteQuietly(Path dir) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best-effort cleanup
                }
            });
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }
}