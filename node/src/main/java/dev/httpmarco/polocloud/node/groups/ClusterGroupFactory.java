package dev.httpmarco.polocloud.node.groups;

import dev.httpmarco.polocloud.api.groups.ClusterGroupService;
import dev.httpmarco.polocloud.api.packet.group.GroupCreatePacket;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

@Log4j2
@UtilityClass
public final class ClusterGroupFactory {

    public void createLocalStorageGroup(@NotNull GroupCreatePacket packet, @NotNull ClusterGroupService clusterGroupService) {
        var group = new ClusterGroupImpl(
                packet.name(),
                packet.platformGroupDisplay(),
                packet.nodes(),
                packet.minMemory(),
                packet.maxMemory(),
                packet.staticService(),
                packet.minOnline(),
                packet.maxOnline()
        );
        clusterGroupService.groups().add(group);
    }
}
