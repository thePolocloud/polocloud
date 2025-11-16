package dev.httpmarco.polocloud.sdk.java.player;

import dev.httpmarco.polocloud.common.future.FutureConverterKt;
import dev.httpmarco.polocloud.shared.player.PolocloudPlayer;
import dev.httpmarco.polocloud.shared.player.SharedPlayerActor;
import dev.httpmarco.polocloud.shared.player.SharedPlayerProvider;
import dev.httpmarco.polocloud.shared.service.Service;
import dev.httpmarco.polocloud.v1.player.*;
import io.grpc.ManagedChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class PlayerProvider implements SharedPlayerProvider<PolocloudPlayer> {

    private final PlayerActor actor = new PlayerActor();
    private final PlayerControllerGrpc.PlayerControllerBlockingStub blockingStub;
    private final PlayerControllerGrpc.PlayerControllerFutureStub futureStub;

    public PlayerProvider(ManagedChannel channel) {
        this.blockingStub = PlayerControllerGrpc.newBlockingStub(channel);
        this.futureStub = PlayerControllerGrpc.newFutureStub(channel);
    }

    @Override
    public @NotNull List<PolocloudPlayer> findAll() {
        return this.blockingStub.findAll(PlayerFindRequest.getDefaultInstance()).getPlayersList().stream().map(PolocloudPlayer.Companion::bindSnapshot).toList();
    }

    @Override
    public @NotNull CompletableFuture<List<PolocloudPlayer>> findAllAsync() {
        return FutureConverterKt.completableFromGuava(this.futureStub.findAll(PlayerFindRequest.newBuilder().build()), findAllPlayerResponse -> findAllPlayerResponse.getPlayersList().stream().map(PolocloudPlayer.Companion::bindSnapshot).toList());
    }

    @Override
    @Nullable
    public PolocloudPlayer findByName(@NotNull String name) {
        return this.blockingStub.findByName(PlayerFindByNameRequest.newBuilder().setName(name).build()).getPlayersList().stream().map(PolocloudPlayer.Companion::bindSnapshot).findFirst().orElse(null);
    }

    @Override
    @NotNull
    public CompletableFuture<PolocloudPlayer> findByNameAsync(@NotNull String name) {
        return FutureConverterKt.completableFromGuava(this.futureStub.findByName(PlayerFindByNameRequest.newBuilder().setName(name).build()),
                findGroupResponse -> findGroupResponse.getPlayersList().stream().map(PolocloudPlayer.Companion::bindSnapshot).findFirst().orElse(null));
    }

    @Override
    @NotNull
    public List<PolocloudPlayer> findByService(@NotNull String serviceName) {
        return this.blockingStub.findByService(PlayerFindByServiceRequest.newBuilder().setCurrentServiceName(serviceName).build()).getPlayersList().stream().map(PolocloudPlayer.Companion::bindSnapshot).toList();
    }

    @Override
    @NotNull
    public CompletableFuture<List<PolocloudPlayer>> findByServiceAsync(@NotNull Service service) {
        return FutureConverterKt.completableFromGuava(this.futureStub.findByService(PlayerFindByServiceRequest.newBuilder().setCurrentServiceName(service.name()).build()),
                findByServiceRequest -> findByServiceRequest.getPlayersList().stream().map(PolocloudPlayer.Companion::bindSnapshot).toList());
    }

    @Override
    public int playerCount() {
        return this.blockingStub
                .playerCount(PlayerCountRequest.getDefaultInstance())
                .getCount();
    }

    @Override
    public @NotNull SharedPlayerActor playerActor() {
        return actor;
    }
}
