package dev.httpmarco.polocloud.api.events.group;

import dev.httpmarco.polocloud.api.groups.CloudGroup;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class CloudGroupUpdateEvent implements GroupEvent {
    private final CloudGroup cloudGroup;
}
