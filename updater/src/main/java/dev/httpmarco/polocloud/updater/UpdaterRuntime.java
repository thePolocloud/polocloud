package dev.httpmarco.polocloud.updater;

import dev.httpmarco.polocloud.common.PolocloudUrl;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.nio.channels.Channels;

public class UpdaterRuntime {

    private static final String DEFAULT_VERSION = System.getenv("POLOCLOUD_VERSION");
    private static final String LAUNCHER_JAR = "polocloud-launcher.jar";

    public static void main(String[] args) {
        var version = versionFromArgs(args);

        System.out.println("Updating to version " + version + "...");

        var targetFile = new File("../../" + LAUNCHER_JAR);
        var downloadUrl = PolocloudUrl.GITHUB_ORGA + "polocloud/releases/download/" + version + "/" + LAUNCHER_JAR;

        if (!downloadJar(downloadUrl, targetFile)) {
            System.err.println("Update aborted.");
            return;
        }

        if (!startLauncher(targetFile)) {
            System.err.println("Failed to restart launcher.");
        }

        System.out.println("Update completed successfully.");
    }

    private static String versionFromArgs(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--version=")) {
                return arg.substring("--version=".length());
            }
        }

        return DEFAULT_VERSION;
    }

    public static boolean downloadJar(String url, File target) {
        System.out.println("Downloading from: " + url);

        try (var input = new URI(url).toURL().openStream();
             var channel = Channels.newChannel(input);
             var output = new FileOutputStream(target)) {

            output.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
            System.out.println("Download completed: " + target.getAbsolutePath());

            return true;
        } catch (Exception e) {
            System.err.println("Failed to download: " + e.getMessage());
            e.printStackTrace(System.err);

            return false;
        }
    }

    public static boolean startLauncher(File jarFile) {
        if (!jarFile.exists()) {
            System.err.println("Launcher not found: " + jarFile.getAbsolutePath());
            return false;
        }

        try {
            var builder = isWindows()
                    ? new ProcessBuilder("cmd.exe", "/c", "start", "cmd.exe", "/k", "java", "-jar", jarFile.getAbsolutePath())
                    : new ProcessBuilder("java", "-jar", jarFile.getAbsolutePath());

            builder.directory(jarFile.getParentFile());
            builder.inheritIO();

            System.out.println("Starting launcher...");
            builder.start();

            System.exit(0);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to start launcher: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
}
