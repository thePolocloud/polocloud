package dev.httpmarco.polocloud.runner.impl;

import dev.httpmarco.polocloud.runner.CloudRunner;
import dev.httpmarco.polocloud.runner.RunnerBootstrap;
import lombok.SneakyThrows;

import java.nio.file.Path;

public final class CloudInstanceRunner implements CloudRunner {

    private static final Path LOCAL_PATH = Path.of("../../local");

    @Override
    @SneakyThrows
    public void run() {
        var apiFile = LOCAL_PATH.resolve("polocloud-api.jar");
        var instanceFile = LOCAL_PATH.resolve("polocloud-instance.jar");

        RunnerBootstrap.LOADER.addURL(apiFile.toUri().toURL());
        RunnerBootstrap.LOADER.addURL(instanceFile.toUri().toURL());
    }

    @Override
    public String mainEntry() {
        return "dev.httpmarco.polocloud.runner.Instance";
    }

    @Override
    public Path dependencyFolder() {
        return LOCAL_PATH.resolve("dependencies");
    }
}
