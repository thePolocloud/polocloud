package dev.httpmarco.polocloud.runner;

import dev.httpmarco.osgan.networking.client.NettyClient;
import dev.httpmarco.osgan.networking.client.NettyClientBuilder;
import dev.httpmarco.polocloud.api.packets.CloudServiceRegisterPacket;

import java.util.UUID;

public class InstanceClient {

    private final NettyClient client;

    public InstanceClient(String hostname, int port) {
        this.client = new NettyClientBuilder()
                .withHostname("127.0.0.1")
                .onActive(channelTransmit -> {
                    channelTransmit.sendPacket(new CloudServiceRegisterPacket(UUID.fromString(System.getenv("serviceId"))));
                }).onInactive(channelTransmit -> {
                    System.out.println("b");
                }).withConnectTimeout(10000).build();
    }
}
