package dev.httpmarco.polocloud.api.events.service;

import dev.httpmarco.polocloud.api.player.CloudPlayer;
import dev.httpmarco.polocloud.api.services.CloudService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@AllArgsConstructor
@Accessors(fluent = true)
public final class CloudServiceSwitchPlayerAmountEvent implements ServiceEvent {

    private final CloudService cloudService;
    private final List<CloudPlayer> cloudPlayers;

    public int amount(){
        return cloudPlayers.size();
    }
}
