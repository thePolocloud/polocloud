package dev.httpmarco.polocloud.node.services.util;

import dev.httpmarco.polocloud.node.Node;
import lombok.experimental.UtilityClass;

import java.net.InetSocketAddress;
import java.net.ServerSocket;

@UtilityClass
public final class ServicePortDetector {

    public int detectServicePort() {
        var serverPort = 30000;

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
