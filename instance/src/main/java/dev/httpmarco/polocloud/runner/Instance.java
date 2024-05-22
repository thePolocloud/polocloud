package dev.httpmarco.polocloud.runner;

import dev.httpmarco.polocloud.api.CloudAPI;
import dev.httpmarco.polocloud.api.dependencies.Dependency;
import dev.httpmarco.polocloud.api.groups.CloudGroupProvider;
import dev.httpmarco.polocloud.api.node.NodeService;
import dev.httpmarco.polocloud.api.services.CloudServiceProvider;
import lombok.SneakyThrows;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.jar.JarFile;

public class Instance extends CloudAPI {

    private final InstanceClient client;

    public static void main(String[] args) {
        //start platform
        new Instance(args);
    }

    @SneakyThrows
    public Instance(String[] args) {

        dependencyService().load(new Dependency("dev.httpmarco", "osgan-utils", "1.1.19-SNAPSHOT", "1.1.19-20240521.201941-1", Dependency.MAVEN_CENTRAL_SNAPSHOT_REPO));
        dependencyService().load(new Dependency("dev.httpmarco", "osgan-files", "1.1.19-SNAPSHOT", "1.1.19-20240521.201941-1", Dependency.MAVEN_CENTRAL_SNAPSHOT_REPO));
        dependencyService().load(new Dependency("dev.httpmarco", "osgan-netty", "1.1.19-SNAPSHOT", "1.1.19-20240521.201941-1", Dependency.MAVEN_CENTRAL_SNAPSHOT_REPO));

        dependencyService().load(new Dependency("io.netty", "netty5-common", "5.0.0.Alpha5"));
        dependencyService().load(new Dependency("io.netty", "netty5-buffer", "5.0.0.Alpha5"));
        dependencyService().load(new Dependency("io.netty", "netty5-codec", "5.0.0.Alpha5"));
        dependencyService().load(new Dependency("io.netty", "netty5-transport", "5.0.0.Alpha5"));
        dependencyService().load(new Dependency("io.netty", "netty5-resolver", "5.0.0.Alpha5"));
        dependencyService().load(new Dependency("io.netty", "netty5-transport-classes-epoll", "5.0.0.Alpha5"));


        dependencyService().load(new Dependency("com.google.code.gson", "gson", "2.10.1"));

        var bootstrapPath = Path.of(Arrays.stream(args).filter(it -> it.startsWith("--bootstrap=")).map(it -> it.substring("--bootstrap=".length())).findFirst().orElse(null) + ".jar");

        this.client = new InstanceClient("127.0.0.1", 8192);

        RunnerBootstrap.LOADER.addURL(bootstrapPath.toUri().toURL());

        try (final var jar = new JarFile(bootstrapPath.toFile())) {
            final var mainClass = jar.getManifest().getMainAttributes().getValue("Main-Class");
            try {
                final var main = Class.forName(mainClass, true, RunnerBootstrap.LOADER).getMethod("main", String[].class);
                main.invoke(null, (Object) Arrays.copyOfRange(args, 2, args.length));
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }


    @Override
    public NodeService nodeService() {
        return null;
    }

    @Override
    public CloudGroupProvider groupProvider() {
        return null;
    }

    @Override
    public CloudServiceProvider serviceProvider() {
        return null;
    }
}
