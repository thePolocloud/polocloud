package dev.httpmarco.polocloud.sdk.java.player;

import dev.httpmarco.polocloud.shared.player.ActorResult;
import dev.httpmarco.polocloud.shared.player.PolocloudPlayer;
import dev.httpmarco.polocloud.shared.player.SharedPlayerActor;
import dev.httpmarco.polocloud.v1.player.*;
import org.jetbrains.annotations.NotNull;

public record PlayerActor(PlayerControllerGrpc.PlayerControllerBlockingStub blockingStub) implements SharedPlayerActor {

    @NotNull
    @Override
    public ActorResult message(@NotNull PolocloudPlayer player, @NotNull String message) {
        var response = blockingStub.message(PlayerActorMessage.newBuilder().setMessage(message).setUniqueId(String.valueOf(player.getUniqueId())).build());
        return new ActorResult(response.getSuccess(), response.getErrorMessage());
    }

    @NotNull
    @Override
    public ActorResult toServer(@NotNull PolocloudPlayer player, @NotNull String serverName) {
        var response = blockingStub.toServer(PlayerActorSend.newBuilder().setService(serverName).setUniqueId(String.valueOf(player.getUniqueId())).build());
        return new ActorResult(response.getSuccess(), response.getErrorMessage());
    }

    @NotNull
    @Override
    public ActorResult kick(@NotNull PolocloudPlayer player, @NotNull String reason) {
        var response = blockingStub.kick(PlayerActorKick.newBuilder().setMessage(reason).setUniqueId(String.valueOf(player.getUniqueId())).build());
        return new ActorResult(response.getSuccess(), response.getErrorMessage());
    }
}
