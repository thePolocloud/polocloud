package dev.httpmarco.polocloud.node.groups.responder;

import dev.httpmarco.osgan.networking.CommunicationProperty;
import dev.httpmarco.osgan.networking.packet.Packet;
import dev.httpmarco.polocloud.api.groups.ClusterGroupService;
import dev.httpmarco.polocloud.api.packet.group.GroupCreatePacket;
import dev.httpmarco.polocloud.api.packet.group.GroupCreationResponse;
import dev.httpmarco.polocloud.api.platforms.PlatformGroupDisplay;
import dev.httpmarco.polocloud.node.cluster.ClusterService;
import dev.httpmarco.polocloud.node.util.JsonUtils;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

@Log4j2
@UtilityClass
public class GroupCreationResponder {

    public static String TAG = "group-creation";

    public Packet handle(@NotNull ClusterGroupService groupService, ClusterService clusterService, @NotNull CommunicationProperty property) {

        var name = property.getString("name");

        if (groupService.exists(name)) {
            return GroupCreationResponse.fail("Group already exists!");
        }

        // alert on every node the new group
        clusterService.broadcastAll(new GroupCreatePacket(
                name,
                JsonUtils.GSON.fromJson(property.getString("nodes"), String[].class),
                new PlatformGroupDisplay(property.getString("platform"), property.getString("version")),
                property.getInteger("minMemory"),
                property.getInteger("maxMemory"),
                property.getBoolean("staticService"),
                property.getInteger("minOnline"),
                property.getInteger("maxOnline"))
        );

        return GroupCreationResponse.success();
    }
}
