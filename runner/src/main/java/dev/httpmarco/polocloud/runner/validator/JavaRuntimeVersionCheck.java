package dev.httpmarco.polocloud.runner.validator;

/**
 * Utility class for validating the currently running Java runtime version.
 * <p>
 * This implementation is compatible with Java 8 and therefore relies on
 * parsing the {@code java.version} system property.
 */
public final class JavaRuntimeVersionCheck implements BootReasonValidator {

    /**
     * The minimum required Java major version.
     */
    private static final int MIN_MAJOR_VERSION = 25;

    /**
     * Extracts the major Java version from the {@code java.version}
     * system property.
     * <p>
     * Supported formats:
     * <ul>
     *   <li>{@code 1.8.0_381} (Java 8 and earlier)</li>
     *   <li>{@code 9}</li>
     *   <li>{@code 17.0.2}</li>
     *   <li>{@code 25-ea}</li>
     * </ul>
     *
     * @return the detected major Java version, or {@code -1} if the version
     * string could not be parsed
     */
    private static int majorJavaVersion() {
        String version = System.getProperty("java.version");

        if (version == null || version.isEmpty()) {
            return -1;
        }

        if (version.startsWith("1.")) {
            return parseIntSafe(version.substring(2, 3));
        }

        // Java 9+: "9", "17.0.2", "25-ea"
        int dotIndex = version.indexOf('.');
        int dashIndex = version.indexOf('-');

        int endIndex = dotIndex > -1 ? dotIndex : dashIndex > -1 ? dashIndex : version.length();
        return parseIntSafe(version.substring(0, endIndex));
    }

    /**
     * Safely parses an integer value.
     *
     * @param value the string value to parse
     * @return the parsed integer, or {@code -1} if parsing fails
     */
    private static int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    @Override
    public boolean isValid() {
        return majorJavaVersion() >= MIN_MAJOR_VERSION;
    }

    @Override
    public String failureMessage() {
        return "The current Java runtime version is not supported. Please use Java " + MIN_MAJOR_VERSION + " or higher.";
    }
}
