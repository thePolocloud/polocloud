package dev.httpmarco.polocloud.api.events.group;

import dev.httpmarco.polocloud.api.groups.CloudGroup;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@AllArgsConstructor
@Accessors(fluent = true)
public final class CloudGroupUpdateEvent implements GroupEvent {

    private final CloudGroup cloudGroup;
}
