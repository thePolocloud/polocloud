package dev.httpmarco.polocloud.launcher;

import dev.httpmarco.polocloud.launcher.boot.NodeBoot;
import dev.httpmarco.polocloud.launcher.dependency.Dependency;
import dev.httpmarco.polocloud.launcher.dependency.DependencyDownloader;
import dev.httpmarco.polocloud.launcher.dependency.Repository;
import dev.httpmarco.polocloud.launcher.util.FileSystemUtils;
import lombok.SneakyThrows;

import java.nio.file.Path;

public final class PoloCloudLauncher {

    public static PoloCloudClassLoader CLASS_LOADER = new PoloCloudClassLoader();

    @SneakyThrows
    public static void main(String[] args) {

        var gsonDependency = new Dependency("com.google.code.gson", "gson", "2.11.0");
        var nettyCommonDependency = new Dependency("io.netty", "netty5-common", "5.0.0.Alpha5");
        var nettyTransportDependency = new Dependency("io.netty", "netty5-transport", "5.0.0.Alpha5");
        var nettyCodecDependency = new Dependency("io.netty", "netty5-codec", "5.0.0.Alpha5");
        var nettyResolverDependency = new Dependency("io.netty", "netty5-resolver", "5.0.0.Alpha5");
        var nettyBufferDependency = new Dependency("io.netty", "netty5-buffer", "5.0.0.Alpha5");
        var nettyTransportEpollDependency = new Dependency("io.netty", "netty5-transport-classes-epoll", "5.0.0.Alpha5");
        var osganNettyDependency = new Dependency("dev.httpmarco", "osgan-netty", "1.2.16-SNAPSHOT", "1.2.16-20240718.130339-1", Repository.MAVEN_CENTRAL_SNAPSHOT);

        // copy cluster api in classpath
        var apiFile = Path.of("local/dependencies/polocloud-api.jar");
        FileSystemUtils.copyClassPathFile(ClassLoader.getSystemClassLoader(), "polocloud-api.jar", apiFile.toString());
        CLASS_LOADER.addURL(apiFile.toFile().toURI().toURL());

        // todo change for service
        var boot = new NodeBoot();

        // add boot file to the current classpath
        CLASS_LOADER.addURL(boot.bootFile().toURI().toURL());

        DependencyDownloader.download(gsonDependency, nettyCommonDependency, nettyTransportDependency, nettyCodecDependency, nettyResolverDependency, nettyBufferDependency, nettyTransportEpollDependency, osganNettyDependency);

        Thread.currentThread().setContextClassLoader(CLASS_LOADER);

        Class.forName(boot.mainClass(), true, CLASS_LOADER).getMethod("main", String[].class).invoke(null, (Object) args);
    }
}
