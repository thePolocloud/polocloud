package dev.httpmarco.polocloud.launcher;

import dev.httpmarco.polocloud.launcher.dependencies.DependencyProvider;
import dev.httpmarco.polocloud.launcher.lib.PolocloudLib;
import dev.httpmarco.polocloud.launcher.lib.PolocloudLibNotFoundException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PolocloudProcess extends Thread {

    private final List<PolocloudLib> processLibs;

    public PolocloudProcess() {
        setDaemon(false);

        this.processLibs = PolocloudLib.of(PolocloudParameters.REQUIRED_LIBS);
        this.processLibs.forEach(PolocloudLib::copyFromClasspath);
    }

    @Override
    public void run() {
        var dependencyProvider = new DependencyProvider();
        dependencyProvider.download();

        var processBuilder = new ProcessBuilder()
                .inheritIO()
                .command(arguments(dependencyProvider));

        // copy all environment variables from the current process
        String version = System.getProperty(PolocloudParameters.VERSION_ENV_ID);
        if (version != null) {
            processBuilder.environment().put(PolocloudParameters.VERSION_ENV_ID, version);
        }

        try {
            Process process = processBuilder.start();
            process.waitFor();
            process.exitValue();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(System.err);
        }
    }

    private List<String> arguments(DependencyProvider dependencyProvider) {
        var arguments = new ArrayList<String>();
        var usedJava = System.getenv("java.home");

        var bootLib = processLibs.stream()
                .filter(lib -> PolocloudParameters.BOOT_LIB.equals(lib.name()))
                .findFirst()
                .orElseThrow(() -> new PolocloudLibNotFoundException(PolocloudParameters.BOOT_LIB));

        arguments.add(usedJava != null ? usedJava + "/bin/java" : "java");
        arguments.add(String.format("-javaagent:%s", bootLib.target()));

        arguments.add("-cp");
        var classpathSeparator = windowsProcess() ? ";" : ":";
        var libClasspath = processLibs
                .stream().map(it -> it.target().toString())
                .collect(Collectors.joining(classpathSeparator));

        var dependencyClasspath = dependencyProvider.dependencies()
                .stream()
                .map(it -> PolocloudParameters.DEPENDENCY_DIRECTORY.resolve(it.file()).toString())
                .collect(Collectors.joining(classpathSeparator));

        arguments.add(libClasspath + classpathSeparator + dependencyClasspath);
        arguments.add(bootLib.mainClass());

        return arguments;
    }

    private boolean windowsProcess() {
        return System.getProperty("os.name").toLowerCase(Locale.getDefault()).contains("win");
    }
}
