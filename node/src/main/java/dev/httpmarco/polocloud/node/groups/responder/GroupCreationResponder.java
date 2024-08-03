package dev.httpmarco.polocloud.node.groups.responder;

import dev.httpmarco.osgan.networking.CommunicationProperty;
import dev.httpmarco.osgan.networking.packet.Packet;
import dev.httpmarco.polocloud.api.groups.ClusterGroupProvider;
import dev.httpmarco.polocloud.api.packet.group.GroupCreatePacket;
import dev.httpmarco.polocloud.api.packet.MessageResponsePacket;
import dev.httpmarco.polocloud.api.platforms.PlatformGroupDisplay;
import dev.httpmarco.polocloud.node.cluster.ClusterService;
import dev.httpmarco.polocloud.node.util.JsonUtils;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

@Log4j2
@UtilityClass
public class GroupCreationResponder {

    public Packet handle(@NotNull ClusterGroupProvider groupService, ClusterService clusterService, @NotNull CommunicationProperty property) {

        var name = property.getString("name");

        if (groupService.exists(name)) {
            return MessageResponsePacket.fail("Group already exists!");
        }

        var minMemory = property.getInteger("minMemory");
        var maxMemory = property.getInteger("maxMemory");

        if (minMemory < 1 || maxMemory < 1) {
            return MessageResponsePacket.fail("The min and max memory musst be higher than 1 mb.");
        }

        if (minMemory > maxMemory) {
            return MessageResponsePacket.fail("The min memory cannot be higher than max memory.");
        }

        var nodes = JsonUtils.GSON.fromJson(property.getString("nodes"), String[].class);
        var platform = new PlatformGroupDisplay(property.getString("platform"), property.getString("version"));

        // alert on every node the new group
        clusterService.broadcastAll(new GroupCreatePacket(
                name,
                nodes,
                platform,
                minMemory,
                maxMemory,
                property.getBoolean("staticService"),
                property.getInteger("minOnline"),
                property.getInteger("maxOnline"))
        );

        return MessageResponsePacket.success();
    }
}
