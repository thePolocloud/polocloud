package dev.httpmarco.polocloud.node.groups.responder;

import dev.httpmarco.osgan.networking.CommunicationProperty;
import dev.httpmarco.osgan.networking.packet.Packet;
import dev.httpmarco.polocloud.api.groups.ClusterGroupService;
import dev.httpmarco.polocloud.api.packet.group.GroupCreationResponse;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class GroupCreationResponder {

    public static String TAG = "group-creation";

    public Packet handle(@NotNull ClusterGroupService groupService, CommunicationProperty property) {

        var name = property.getString("name");

        if (groupService.exists(name)) {
            return GroupCreationResponse.fail("Group already exists!");
        }

        return GroupCreationResponse.success();
    }
}
