package dev.httpmarco.polocloud.api.events.player;

import dev.httpmarco.polocloud.api.player.CloudPlayer;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class CloudPlayerSetPropertyEvent implements CloudPlayerEvent {
    private final CloudPlayer cloudPlayer;

    //todo: Add changed property
}
