package dev.httpmarco.polocloud.api.packet.group;

import dev.httpmarco.polocloud.api.packets.resources.AbstractStringComponentPacket;

public final class GroupDeletePacket extends AbstractStringComponentPacket {

    public GroupDeletePacket(String content) {
        super(content);
    }

    public String name() {
        return this.content();
    }
}
