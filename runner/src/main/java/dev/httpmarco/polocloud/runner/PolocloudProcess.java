package dev.httpmarco.polocloud.runner;

import dev.httpmarco.polocloud.runner.classloader.PolocloudClassLoader;
import dev.httpmarco.polocloud.runner.expender.ExpenderRuntimeCache;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Controls the lifecycle of the Polocloud CLI process.
 * <p>
 * The CLI is executed inside the current JVM using a dedicated
 * {@link PolocloudClassLoader} to allow dynamic dependency management
 * and clean restarts without spawning a new JVM.
 *
 * <p>
 * This class is fully compatible with Java 8.
 */
public final class PolocloudProcess {

    /**
     * Starts the Polocloud CLI.
     *
     * @return {@code 0} if the CLI started successfully, {@code 1} otherwise
     */
    public int start() {
        PolocloudClassLoader classLoader = null;

        try {
            prepareRuntimeEnvironment();

            classLoader = createApplicationClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);

            invokeMain(classLoader);
            return 0;

        } catch (InvocationTargetException e) {
            System.err.println("Polocloud CLI terminated with an exception:");
            e.getTargetException().printStackTrace(System.err);
        } catch (Exception e) {
            System.err.println("Failed to start Polocloud CLI");
            e.printStackTrace(System.err);
        } finally {
            closeClassLoaderQuietly(classLoader);
        }

        return 1;
    }

    /**
     * Prepares all required runtime components before the CLI is started.
     *
     * @throws IOException if runtime preparation fails
     */
    private void prepareRuntimeEnvironment() throws IOException {
        ExpenderRuntimeCache.migrateCacheFiles();
        ensureKotlinRuntimePresent();
    }

    /**
     * Creates the {@link PolocloudClassLoader} used to execute the CLI.
     *
     * @return a new application class loader
     * @throws IOException if classpath URLs cannot be created
     */
    private PolocloudClassLoader createApplicationClassLoader() throws IOException {
        List<Path> classpath = getApplicationClasspath();
        List<URL> urls = new ArrayList<>(classpath.size());

        for (Path path : classpath) {
            urls.add(path.toUri().toURL());
        }

        return new PolocloudClassLoader(
                urls.toArray(new URL[0]),
                ClassLoader.getSystemClassLoader()
        );
    }

    /**
     * Resolves the classpath entries required by the CLI.
     *
     * @return the CLI classpath
     */
    private List<Path> getApplicationClasspath() {
        return Arrays.asList(
                PolocloudParameters.BOOT_KOTLIN,
                PolocloudParameters.expenderRuntimeCache("cli"),
                PolocloudParameters.expenderRuntimeCache("common")
        );
    }

    /**
     * Invokes the CLI main method via reflection.
     *
     * @param classLoader the class loader used to load the CLI
     * @throws Exception if the main method cannot be invoked
     */
    private void invokeMain(ClassLoader classLoader) throws Exception {
        String mainClassName = resolveMainClassName();

        Class<?> mainClass = Class.forName(mainClassName, true, classLoader);
        Method mainMethod = mainClass.getMethod("main", String[].class);

        mainMethod.invoke(null, (Object) new String[0]);
    }

    /**
     * Resolves the fully qualified main class name of the CLI artifact.
     *
     * @return the CLI main class name
     */
    private String resolveMainClassName() {
        return Objects.requireNonNull(
                ExpenderRuntimeCache.findElementByArtifactId("cli"),
                "CLI artifact not found in runtime cache"
        ).mainClass();
    }

    /**
     * Ensures that the Kotlin runtime JAR is available locally.
     *
     * @throws IOException if the runtime cannot be downloaded
     */
    private void ensureKotlinRuntimePresent() throws IOException {
        Path kotlinJar = PolocloudParameters.BOOT_KOTLIN;

        if (Files.exists(kotlinJar)) {
            return;
        }

        Files.createDirectories(kotlinJar.getParent());
        downloadKotlinRuntime(kotlinJar);
    }

    /**
     * Downloads the Kotlin runtime JAR.
     *
     * @param target the destination path
     * @throws IOException if downloading fails
     */
    private void downloadKotlinRuntime(Path target) throws IOException {
        try (InputStream in = new URL(PolocloudParameters.KOTLIN_DOWNLOAD_URL).openStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Closes the class loader quietly.
     *
     * @param classLoader the class loader to close
     */
    private void closeClassLoaderQuietly(PolocloudClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }

        try {
            classLoader.close();
        } catch (Exception e) {
            System.err.println("Failed to close PolocloudClassLoader");
            e.printStackTrace(System.err);
        }
    }
}
