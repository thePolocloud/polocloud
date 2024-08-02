package dev.httpmarco.polocloud.node.services;

import dev.httpmarco.polocloud.api.groups.ClusterGroup;
import dev.httpmarco.polocloud.api.services.ClusterService;
import dev.httpmarco.polocloud.api.services.ClusterServiceFactory;
import dev.httpmarco.polocloud.node.Node;
import dev.httpmarco.polocloud.node.packets.resources.services.ClusterSyncRegisterServicePacket;
import dev.httpmarco.polocloud.node.services.util.ServicePortDetector;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.stream.IntStream;

@Log4j2
public final class ClusterServiceFactoryImpl implements ClusterServiceFactory {

    @Override
    @SneakyThrows
    public void runGroupService(ClusterGroup group) {
        var runningNode = Node.instance().clusterService().localNode().data();

        var localService = new ClusterLocalServiceImpl(group, generateOrderedId(group), UUID.randomUUID(), ServicePortDetector.detectServicePort(), "0.0.0.0", runningNode.name());
        log.info("The service &8'&f{}&8' &7is starting now&8...", localService.name());
        Node.instance().serviceProvider().services().add(localService);

        // call other nodes
        Node.instance().clusterService().broadcast(new ClusterSyncRegisterServicePacket(localService));

        // create process
        var processBuilder = new ProcessBuilder();


        //localService.process(processBuilder.start());
    }

    // broadcast service sync
    //
    // boot launcher

    @Override
    public void shutdownGroupService(ClusterService clusterService) {

    }

    private int generateOrderedId(ClusterGroup group) {
        return IntStream.iterate(1, i -> i + 1).filter(id -> !isIdPresent(group, id)).findFirst().orElseThrow();
    }

    private boolean isIdPresent(@NotNull ClusterGroup group, int id) {
        return group.services().stream().anyMatch(it -> it.orderedId() == id);
    }
}
