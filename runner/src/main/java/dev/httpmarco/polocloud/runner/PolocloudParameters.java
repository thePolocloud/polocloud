package dev.httpmarco.polocloud.runner;

/**
 * Holds constant parameter and environment variable names
 * used by the Polocloud runner.
 *
 * <p>This class is not intended to be instantiated.</p>
 */
public final class PolocloudParameters {

    /**
     * Environment variable that defines the Polocloud version
     * used by the runner.
     *
     * <p>Example value: {@code 1.0.0}</p>
     */
    public static final String VERSION_ENV = "version";

    /**
     * Private constructor to prevent instantiation.
     */
    private PolocloudParameters() {
        throw new UnsupportedOperationException("This is a utility class");
    }
}
