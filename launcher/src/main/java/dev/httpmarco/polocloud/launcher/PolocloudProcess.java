package dev.httpmarco.polocloud.launcher;

import java.io.IOException;

public final class PolocloudProcess {

    private Process process;


    public void start() {
        ProcessBuilder processBuilder = new ProcessBuilder()
                .command("java", "-jar", "polocloud-server.jar")
                .inheritIO();

        try {
            this.process = processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    public int waitForExit() {
        try {
            return this.process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace(System.err);
            return -1;
        }
    }
}
