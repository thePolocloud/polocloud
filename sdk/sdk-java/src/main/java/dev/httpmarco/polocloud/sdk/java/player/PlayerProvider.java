package dev.httpmarco.polocloud.sdk.java.player;

import com.google.protobuf.Empty;
import com.google.protobuf.InvalidProtocolBufferException;
import dev.httpmarco.polocloud.common.future.FutureConverterKt;
import dev.httpmarco.polocloud.shared.player.PolocloudPlayer;
import dev.httpmarco.polocloud.shared.player.SharedPlayerProvider;
import dev.httpmarco.polocloud.shared.service.Service;
import dev.httpmarco.polocloud.v1.player.*;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerProvider implements SharedPlayerProvider<PolocloudPlayer> {

    private PlayerActorAdapter actorAdapter = new EmptyPlayerActor();
    private final PlayerControllerGrpc.PlayerControllerBlockingStub blockingStub;
    private final PlayerControllerGrpc.PlayerControllerStub playerActorStub;
    private final PlayerControllerGrpc.PlayerControllerFutureStub futureStub;

    public PlayerProvider(ManagedChannel channel) {
        this.blockingStub = PlayerControllerGrpc.newBlockingStub(channel);
        this.playerActorStub = PlayerControllerGrpc.newStub(channel);
        this.futureStub = PlayerControllerGrpc.newFutureStub(channel);

        playerActorStub.withWaitForReady().registerActor(Empty.getDefaultInstance(), new StreamObserver<>() {
            @Override
            public void onNext(PlayerActorRegister value) {
                try {
                    if (value.getType() == PlayerActorType.KICK) {
                        PlayerActorKick actorKick = value.getContent().unpack(PlayerActorKick.class);
                        actorAdapter.kick(UUID.fromString(actorKick.getUuid()), actorKick.getMessage());
                    } else if (value.getType() == PlayerActorType.MESSAGE) {
                        PlayerActorMessage actorMessage = value.getContent().unpack(PlayerActorMessage.class);
                        actorAdapter.kick(UUID.fromString(actorMessage.getUuid()), actorMessage.getMessage());
                    } else if (value.getType() == PlayerActorType.SEND) {
                        PlayerActorSend actorSend = value.getContent().unpack(PlayerActorSend.class);
                        actorAdapter.kick(UUID.fromString(actorSend.getUuid()), actorSend.getServiceId());
                    }
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {

            }
        });
    }

    @Override
    public @NotNull List<PolocloudPlayer> findAll() {
        return this.blockingStub.findAll(Empty.getDefaultInstance()).getPlayersList().stream().map(PolocloudPlayer.Companion::bindSnapshot).toList();
    }

    @Override
    public @NotNull CompletableFuture<List<PolocloudPlayer>> findAllAsync() {
        return FutureConverterKt.completableFromGuava(this.futureStub.findAll(Empty.getDefaultInstance()), findAllPlayerResponse -> findAllPlayerResponse.getPlayersList().stream().map(PolocloudPlayer.Companion::bindSnapshot).toList());
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
        return this.blockingStub.playerCount(Empty.getDefaultInstance()).getCount();
    }

    public void updateActorAdapter(@NotNull PlayerActorAdapter adapter) {
        this.actorAdapter = adapter;
    }
}
