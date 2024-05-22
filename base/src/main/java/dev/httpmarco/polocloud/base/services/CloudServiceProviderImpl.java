package dev.httpmarco.polocloud.base.services;

import dev.httpmarco.polocloud.api.groups.CloudGroup;
import dev.httpmarco.polocloud.api.services.CloudService;
import dev.httpmarco.polocloud.api.services.CloudServiceFactory;
import dev.httpmarco.polocloud.api.services.CloudServiceProvider;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Accessors(fluent = true)
public final class CloudServiceProviderImpl implements CloudServiceProvider {

    private final List<CloudService> services = new ArrayList<>();
    private final CloudServiceFactory factory = new CloudServiceFactoryImpl();
    private final CloudServiceQueue queue = new CloudServiceQueue(this);

    public CloudServiceProviderImpl() {
        queue.start();
    }

    public void close() {
        queue.interrupt();
    }

    public void registerService(CloudService cloudService) {
        this.services.add(cloudService);
    }

    @Override
    public List<CloudService> services(CloudGroup group) {
        return services.stream().filter(it -> it.group().equals(group)).toList();
    }

    @Override
    public CloudService find(UUID id) {
        return services.stream().filter(it -> it.id().equals(id)).findFirst().orElse(null);
    }
}
