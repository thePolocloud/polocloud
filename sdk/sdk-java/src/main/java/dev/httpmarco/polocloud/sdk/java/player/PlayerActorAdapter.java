package dev.httpmarco.polocloud.sdk.java.player;

import java.util.UUID;

public interface PlayerActorAdapter {

    void message(UUID uuid, String message);

    void kick(UUID uuid, String message);

    void toServer(UUID uuid, String service);

}
