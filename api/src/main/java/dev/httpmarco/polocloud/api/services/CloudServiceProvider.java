package dev.httpmarco.polocloud.api.services;

import dev.httpmarco.polocloud.api.groups.CloudGroup;

import java.util.List;
import java.util.UUID;

public interface CloudServiceProvider {

    CloudServiceFactory factory();

    List<CloudService> services();

    List<CloudService> services(CloudGroup group);

    CloudService find(UUID id);

}
