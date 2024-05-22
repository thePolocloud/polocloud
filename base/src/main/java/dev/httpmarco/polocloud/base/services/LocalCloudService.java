package dev.httpmarco.polocloud.base.services;

import dev.httpmarco.polocloud.api.groups.CloudGroup;
import dev.httpmarco.polocloud.base.CloudBase;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.nio.file.Path;
import java.util.UUID;

@Getter
@Accessors(fluent = true)
public final class LocalCloudService extends CloudServiceImpl {

    private final Path runningFolder;

    @Setter
    private Process process;

    public LocalCloudService(CloudGroup group, int orderedId, UUID id) {
        super(group, orderedId, id);

        this.runningFolder = Path.of(CloudBase.instance().configuration().runningDirectory() + name() + "-" + id());
    }
}
