package dev.httpmarco.polocloud.runner;

import dev.httpmarco.polocloud.runner.expender.ExpenderRuntimeCache;

final class PolocloudRuntimeLauncher {

    private PolocloudRuntimeLauncher() {
    }

    static void main(String[] args) {
        PolocloudRuntimeBootValidator validator = new PolocloudRuntimeBootValidator();

        if (!validator.isValid()) {
            System.exit(1);
            return;
        }

        PolocloudProcess process = new PolocloudProcess();
        process.start();
        System.exit(process.waitForExit());
    }
}
