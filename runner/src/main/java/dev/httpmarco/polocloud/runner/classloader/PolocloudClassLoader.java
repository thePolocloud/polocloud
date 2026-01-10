package dev.httpmarco.polocloud.runner.classloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Dedicated class loader used to run the Polocloud CLI inside the launcher JVM.
 * <p>
 * This class loader is intentionally isolated to allow dynamic dependency
 * resolution, updates and restarts without restarting the JVM.
 */
public final class PolocloudClassLoader extends URLClassLoader {

    static {
        /*
         * Required for Java 8 to allow proper class unloading
         * when the class loader is no longer referenced.
         */
        ClassLoader.registerAsParallelCapable();
    }

    /**
     * Creates a new Polocloud class loader.
     *
     * @param urls   the classpath URLs
     * @param parent the parent class loader
     */
    public PolocloudClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    /**
     * Closes this class loader and releases all associated resources.
     *
     */
    @Override
    public void close() throws IOException {
        super.close();
    }
}
