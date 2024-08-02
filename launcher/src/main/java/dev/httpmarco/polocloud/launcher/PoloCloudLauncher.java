package dev.httpmarco.polocloud.launcher;

import dev.httpmarco.polocloud.launcher.boot.NodeBoot;
import dev.httpmarco.polocloud.launcher.dependency.Dependency;
import dev.httpmarco.polocloud.launcher.dependency.DependencyDownloader;
import lombok.SneakyThrows;

public final class PoloCloudLauncher {

    @SneakyThrows
    public static void main(String[] args) {

        var classLoader = new PoloCloudClassLoader();

        var guiceDependency = new Dependency("com.google.inject", "guice", "7.0.0");
        var gsonDependency = new Dependency("com.google.code.gson", "gson", "2.11.0");

        // todo change for service
        var boot = new NodeBoot();

        // add boot file to the current classpath
        classLoader.addURL(boot.bootFile().toURI().toURL());

        DependencyDownloader.download(guiceDependency, gsonDependency);

        Thread.currentThread().setContextClassLoader(classLoader);
        Class.forName(boot.mainClass(), true, classLoader).getMethod("main", String[].class).invoke(null, (Object) args);
    }
}
