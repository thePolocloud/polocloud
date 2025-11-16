package dev.httpmarco.polocloud.sdk.java.player;

import dev.httpmarco.polocloud.shared.player.SharedPlayerActor;
import org.jetbrains.annotations.NotNull;

public final class PlayerActor  implements SharedPlayerActor {

    @Override
    public void message(@NotNull String message) {
        // TODO: Implement message sending logic
    }

    @Override
    public void toServer(@NotNull String serverName) {
        // TODO: Implement server transfer logic
    }

    @Override
    public void kick(@NotNull String reason) {
        // TODO: Implement kick logic
    }
}
