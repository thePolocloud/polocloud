package dev.httpmarco.polocloud.runner;

import dev.httpmarco.polocloud.runner.utils.Manifests;

/**
 * Entry point for the PoloCloud runtime.
 * <p>
 * This launcher is responsible for:
 * <ul>
 *     <li>Validating the runtime environment</li>
 *     <li>Resolving the current runtime version from the manifest</li>
 *     <li>Starting the PoloCloud process</li>
 *     <li>Exiting the JVM with the process exit code</li>
 * </ul>
 */
final class PolocloudRuntimeLauncher {

    /**
     * Utility class – instantiation is not allowed.
     */
    private PolocloudRuntimeLauncher() {
        throw new UnsupportedOperationException("This is a boot class");
    }

    /**
     * Launches the PoloCloud runtime.
     *
     * @param args command-line arguments (currently unused)
     */
    public static void main(String[] args) {
        final PolocloudRuntimeBootValidator validator = new PolocloudRuntimeBootValidator();

        if (!validator.isValid()) {
            System.exit(1);
        }

        final String runtimeVersion = Manifests
                .readOwnManifest()
                .getMainAttributes()
                .getValue(PolocloudParameters.VERSION_ENV);

        final PolocloudProcess process = new PolocloudProcess();
        process.start(runtimeVersion);

        System.exit(process.waitForExit());
    }
}
