package dev.httpmarco.polocloud.base.services;

import dev.httpmarco.osgan.files.Files;
import dev.httpmarco.polocloud.api.CloudAPI;
import dev.httpmarco.polocloud.api.groups.CloudGroup;
import dev.httpmarco.polocloud.api.services.CloudService;
import dev.httpmarco.polocloud.api.services.CloudServiceFactory;
import dev.httpmarco.polocloud.base.CloudBase;
import dev.httpmarco.polocloud.base.groups.CloudServiceGroupProvider;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public final class CloudServiceFactoryImpl implements CloudServiceFactory {

    private static final Path RUNNING_FOLDER = Path.of("running");

    public CloudServiceFactoryImpl() {
        Files.createDirectoryIfNotExists(RUNNING_FOLDER);
    }

    @Override
    @SneakyThrows
    public void start(CloudGroup cloudGroup) {
        var service = new LocalCloudService(cloudGroup, this.nextServiceId(cloudGroup), UUID.randomUUID());
        ((CloudServiceProviderImpl) CloudAPI.instance().serviceProvider()).registerService(service);

        CloudAPI.instance().logger().info("The Service &2'&4" + service.name() + "&2' &1is now starting on node &2'&4" + CloudAPI.instance().nodeService().localNode().name() + "&2'&2.");

        Files.createDirectoryIfNotExists(service.runningFolder());

        // download and/or copy platform file to service
        var platformService = ((CloudServiceGroupProvider) CloudBase.instance().groupProvider()).platformService();
        platformService.preparePlatform(service);

        var args = new LinkedList<>();

        args.add("java");
        args.add("-javaagent:../../polocloud.jar");
        args.addAll(Arrays.stream(platformService.find(cloudGroup.platform()).platformsEnvironment()).toList());
        args.add("-jar");
        args.add("../../polocloud.jar");
        args.add("--instance");
        args.add("--bootstrap=" + service.group().platform());
        args.addAll(Arrays.stream(platformService.find(cloudGroup.platform()).platformsArguments()).toList());

        var processBuilder = new ProcessBuilder().directory(service.runningFolder().toFile()).command(args.toArray(String[]::new));

        processBuilder.environment().put("serviceId", service.id().toString());
        service.process(processBuilder.start());
    }

    @Override
    @SneakyThrows
    public void stop(CloudService service) {
        if (!(service instanceof LocalCloudService localCloudService)) {
            return;
        }

        if (localCloudService.process() != null) {
            localCloudService.process().toHandle().destroyForcibly();
            localCloudService.process().waitFor();
            localCloudService.process(null);
        }

        synchronized (this) {
            deleteDirectory(localCloudService.runningFolder().toFile());
        }

        java.nio.file.Files.deleteIfExists(localCloudService.runningFolder());
        CloudAPI.instance().logger().info("The service &2'&4" + service.name() + "&2'&1 has been stopped successfully");
    }

    private int nextServiceId(CloudGroup cloudGroup) {
        int id = 1;
        while (this.isIdPresent(cloudGroup, id)) {
            id++;
        }
        return id;
    }

    private boolean isIdPresent(CloudGroup group, int id) {
        return CloudAPI.instance().serviceProvider().services(group).stream().anyMatch(it -> it.orderedId() == id);
    }

    private void deleteDirectory(File directoryToBeDeleted) {
        var allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (var file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }
}
