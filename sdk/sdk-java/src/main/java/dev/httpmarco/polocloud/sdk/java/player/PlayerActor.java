package dev.httpmarco.polocloud.sdk.java.player;

import dev.httpmarco.polocloud.shared.player.PolocloudPlayer;
import dev.httpmarco.polocloud.shared.player.SharedPlayerActor;
import org.jetbrains.annotations.NotNull;

public final class PlayerActor  implements SharedPlayerActor {

    @Override
    public void message(@NotNull PolocloudPlayer player, @NotNull String message) {

    }

    @Override
    public void toServer(@NotNull PolocloudPlayer player, @NotNull String serverName) {

    }

    @Override
    public void kick(@NotNull PolocloudPlayer player, @NotNull String reason) {

    }
}
