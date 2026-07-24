package de.polocloud.runner;

import de.polocloud.runner.runtime.RuntimeMode;
import de.polocloud.runner.runtime.RuntimeProcess;
import de.polocloud.runner.runtime.RuntimeResolver;
import de.polocloud.runner.runtime.impl.CliRuntimeProcess;
import de.polocloud.runner.runtime.impl.NodeRuntimeProcess;
import de.polocloud.runner.update.UpdateStaging;
import de.polocloud.runner.utils.Manifests;

final class PolocloudRuntimeLauncher {

    private PolocloudRuntimeLauncher() {}

    public static void main(String[] args) {
        long startupTime = System.currentTimeMillis();
        PolocloudParameters.ensureCacheDirectory();

        if (!new PolocloudRuntimeBootValidator().isValid()) {
            System.exit(1);
        }

        String version = Manifests
                .readOwnManifest()
                .getMainAttributes()
                .getValue(PolocloudParameters.VERSION_ENV);

        System.setProperty(PolocloudParameters.VERSION_ENV, version);
        System.setProperty(PolocloudParameters.STARTUP_TIME, String.valueOf(startupTime));
        System.setProperty(PolocloudParameters.LAUNCH_ARGS, String.join(PolocloudParameters.LAUNCH_ARGS_SEPARATOR, args));

        // May override VERSION_ENV to a newer version staged by a previous run's
        // self-update — must run before any module classpath path is resolved.
        UpdateStaging.applyIfPresent();

        parseAndApplyProperties(args);

        RuntimeMode mode = RuntimeResolver.resolve(args);
        RuntimeProcess process = createProcess(mode);

        int status = process.start();
        System.exit(status);
    }

    private static RuntimeProcess createProcess(RuntimeMode mode) {
        switch (mode) {
            case CLI: return new CliRuntimeProcess();
            case NODE: return new NodeRuntimeProcess();
            default: throw new IllegalStateException("Unsupported mode: " + mode);
        }
    }

    private static void parseAndApplyProperties(String[] args) {
        for (String arg : args) {
            if (!arg.startsWith("--")) continue;

            String[] parts = arg.substring(2).split("=", 2);
            if (parts.length != 2) continue;

            switch (parts[0]) {
                case "join-token":
                    System.setProperty(PolocloudParameters.JOIN_TOKEN, parts[1]);
                    break;
                case "join-host":
                    System.setProperty(PolocloudParameters.JOIN_HOST, parts[1]);
                    break;
                case "join-port":
                    System.setProperty(PolocloudParameters.JOIN_PORT, parts[1]);
                    break;
                case "group":
                    System.setProperty(PolocloudParameters.NODE_GROUP, parts[1]);
                    break;
                default:
                    break;
            }
        }
    }
}
