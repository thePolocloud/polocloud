package dev.httpmarco.polocloud.api.events.service;

import dev.httpmarco.polocloud.api.services.CloudService;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class CloudServiceUpdatePropertyEvent implements ServiceEvent{
    private final CloudService cloudService;

    //todo: Add changed property
}
