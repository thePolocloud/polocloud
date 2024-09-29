package dev.httpmarco.polocloud.node.services.util;

import dev.httpmarco.polocloud.api.groups.ClusterGroup;
import dev.httpmarco.polocloud.api.platforms.PlatformType;
import dev.httpmarco.polocloud.api.properties.PropertiesPool;
import dev.httpmarco.polocloud.node.Node;
import dev.httpmarco.polocloud.node.NodeProperties;
import dev.httpmarco.polocloud.node.services.ClusterLocalServiceImpl;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.net.ServerSocket;

@UtilityClass
public final class ServicePortDetector {

    public int detectServicePort(@NotNull ClusterGroup group) {
        var platformType = group.platform().type();
        var serverPort = platformType.defaultRuntimePort();

        var pool = Node.instance().nodeProperties();

        if (group.properties().has(NodeProperties.GROUP_PORT_START_RANGE)) {
            serverPort = group.properties().property(NodeProperties.GROUP_PORT_START_RANGE);
        } else if (platformType == PlatformType.PROXY && pool.has(NodeProperties.PROXY_PORT_START_RANGE)) {
            serverPort = pool.property(NodeProperties.PROXY_PORT_START_RANGE);
        } else if (platformType == PlatformType.SERVER && pool.has(NodeProperties.SERVER_PORT_START_RANGE)) {
            serverPort = pool.property(NodeProperties.SERVER_PORT_START_RANGE);
        } else if (platformType == PlatformType.SERVICE && pool.has(NodeProperties.SERVICE_PORT_START_RANGE)) {
            serverPort = pool.property(NodeProperties.SERVICE_PORT_START_RANGE);
        }

        while (isUsed(serverPort)) {
            serverPort++;
        }

        return serverPort;
    }

    private boolean isUsed(int port) {
        for (var service : Node.instance().serviceProvider().services()) {
            if (service.port() == port) {
                return true;
            }
        }
        try (var testSocket = new ServerSocket()) {
            testSocket.bind(new InetSocketAddress(port));
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}