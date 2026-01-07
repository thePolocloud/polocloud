package dev.httpmarco.polocloud.cli;

final class PolocloudRuntimeLauncher {

    private PolocloudRuntimeLauncher() {
    }

    public static void main(String[] args) {
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
