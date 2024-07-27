package dev.httpmarco.polocloud.node.cluster.impl;

import dev.httpmarco.osgan.networking.CommunicationProperty;
import dev.httpmarco.osgan.networking.channel.ChannelTransmit;
import dev.httpmarco.osgan.networking.packet.Packet;
import dev.httpmarco.osgan.networking.packet.RequestPacket;
import dev.httpmarco.osgan.networking.server.CommunicationServer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;


public class LocalChannelTransmit extends ChannelTransmit {

    private final CommunicationServer server;

    public LocalChannelTransmit(CommunicationServer server) {
        super("local-node", null);
        this.server = server;
    }

    @Override
    public void sendPacket(@NotNull Packet packet) {
        server.call(packet, this);
    }

    @Override
    public void responder(String id, Function<CommunicationProperty, Packet> packetFunction) {
        this.server.responder(id, packetFunction);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <P extends Packet> void request(String id, CommunicationProperty property, Class<P> packet, Consumer<P> packetCallback) {
        var uuid = UUID.randomUUID();
        server.requests().put(uuid, (Consumer<Packet>) packetCallback);
        this.sendPacket(new RequestPacket(id, uuid, property));
    }
}
