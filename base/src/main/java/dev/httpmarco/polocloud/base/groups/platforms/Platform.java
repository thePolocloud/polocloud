package dev.httpmarco.polocloud.base.groups.platforms;

import dev.httpmarco.polocloud.base.services.LocalCloudService;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.Set;

@Getter
@Accessors(fluent = true)
public abstract class Platform {

    private final Set<String> possibleVersions = new HashSet<>();

    public abstract void download(String version);

    public abstract void prepare(LocalCloudService localCloudService);

    public String[] platformsEnvironment() {
        return new String[0];
    }

    public String[] platformsArguments() {
        return new String[0];
    }
}