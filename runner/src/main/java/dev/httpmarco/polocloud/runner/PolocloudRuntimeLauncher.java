package dev.httpmarco.polocloud.runner;

import dev.httpmarco.polocloud.runner.utils.Manifests;

final class PolocloudRuntimeLauncher {

    private PolocloudRuntimeLauncher() {
    }

    public static void main(String[] args) {
        PolocloudRuntimeBootValidator validator = new PolocloudRuntimeBootValidator();

        if (!validator.isValid()) {
            System.exit(1);
            return;
        }

        String runtimeVersion = Manifests.readOwnManifest().getMainAttributes().getValue(PolocloudParameters.VERSION_ENV);

        PolocloudProcess process = new PolocloudProcess();
        process.start(runtimeVersion);
        System.exit(process.waitForExit());
    }
}
