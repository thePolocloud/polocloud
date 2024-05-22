package dev.httpmarco.polocloud.api.events.service;

import dev.httpmarco.polocloud.api.events.Event;
import dev.httpmarco.polocloud.api.services.CloudService;

public interface ServiceEvent extends Event {

    CloudService cloudService();

}
