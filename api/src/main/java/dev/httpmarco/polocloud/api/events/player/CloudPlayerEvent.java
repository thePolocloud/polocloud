package dev.httpmarco.polocloud.api.events.player;

import dev.httpmarco.polocloud.api.events.Event;
import dev.httpmarco.polocloud.api.player.CloudPlayer;

public interface CloudPlayerEvent extends Event {

    CloudPlayer cloudPlayer();
}
