package dev.httpmarco.polocloud.base.groups;

import dev.httpmarco.polocloud.api.CloudAPI;
import dev.httpmarco.polocloud.api.groups.CloudGroup;
import dev.httpmarco.polocloud.api.groups.CloudGroupProvider;
import dev.httpmarco.polocloud.base.CloudBase;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Accessors(fluent = true)
public final class CloudServiceGroupProvider implements CloudGroupProvider {

    private final List<CloudGroup> groups;
    private final CloudGroupServiceTypeAdapter groupServiceTypeAdapter = new CloudGroupServiceTypeAdapter();
    private final CloudGroupPlatformService platformService = new CloudGroupPlatformService();

    public CloudServiceGroupProvider() {
        this.groups = groupServiceTypeAdapter.readGroups();
        CloudBase.instance().logger().info("Loading following groups&2: &1" + String.join(", ", groups.stream().map(CloudGroup::name).toList()));
    }

    @Override
    public void createGroup(String name, String platform, int memory, int minOnlineCount) {

        if (!isGroup(name)) {
            CloudAPI.instance().logger().info("The group already exists!");
            return;
        }

        if (memory <= 0) {
            CloudAPI.instance().logger().info("The minimum memory value must be higher then 0. ");
            return;
        }

        if (!platformService.isValidPlatform(platform)) {
            CloudAPI.instance().logger().info("The platform " + platform + " is an invalid type!");
            return;
        }

        this.groupServiceTypeAdapter.includeFile(new CloudGroupImpl(name, platform, memory, minOnlineCount));
    }

    @Override
    public boolean isGroup(String name) {
        return groups.stream().anyMatch(it -> it.name().equalsIgnoreCase(name));
    }

}
