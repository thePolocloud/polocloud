package dev.httpmarco.polocloud.api.events.group;

import dev.httpmarco.polocloud.api.events.Event;
import dev.httpmarco.polocloud.api.groups.CloudGroup;


public interface GroupEvent extends Event {
    CloudGroup cloudGroup();
}
