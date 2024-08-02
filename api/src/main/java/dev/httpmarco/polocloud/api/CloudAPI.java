package dev.httpmarco.polocloud.api;

import dev.httpmarco.polocloud.api.services.ClusterServiceProvider;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public abstract class CloudAPI {

    @Getter
    private static CloudAPI instance;

    public CloudAPI() {
        instance = this;
    }

    public abstract ClusterServiceProvider serviceProvider();

}
