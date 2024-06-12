/*
 * Copyright 2024 Mirco Lindenau | HttpMarco
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.httpmarco.polocloud.runner.player;

import dev.httpmarco.osgan.networking.CommunicationProperty;
import dev.httpmarco.osgan.networking.packet.PacketBuffer;
import dev.httpmarco.osgan.utils.executers.FutureResult;
import dev.httpmarco.polocloud.api.packets.player.CloudAllPlayersPacket;
import dev.httpmarco.polocloud.api.packets.player.CloudPlayerPacket;
import dev.httpmarco.polocloud.api.packets.player.CloudPlayerRegisterPacket;
import dev.httpmarco.polocloud.api.packets.player.CloudPlayerUnregisterPacket;
import dev.httpmarco.polocloud.api.player.CloudPlayer;
import dev.httpmarco.polocloud.api.player.CloudPlayerProvider;
import dev.httpmarco.polocloud.runner.CloudInstance;
import lombok.SneakyThrows;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class InstanceCloudPlayerProvider implements CloudPlayerProvider {

    @Override
    @SneakyThrows
    public List<CloudPlayer> players() {
        return this.playersAsync().get(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<List<CloudPlayer>> playersAsync() {
        var future = new FutureResult<List<CloudPlayer>>();
        CloudInstance.instance().client().transmitter().request("players-all", CloudAllPlayersPacket.class, it -> future.complete(it.players()));
        return future.toCompletableFuture();
    }

    @Override
    public void register(UUID id, String name) {
        CloudInstance.instance().client().transmitter().sendPacket(new CloudPlayerRegisterPacket(id, name, CloudInstance.SELF_ID));
    }

    @Override
    public void unregister(UUID id) {
        CloudInstance.instance().client().transmitter().sendPacket(new CloudPlayerUnregisterPacket(id));
    }

    @Override
    @SneakyThrows
    public CloudPlayer find(UUID id) {
        return this.findAsync(id).get(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<CloudPlayer> findAsync(UUID id) {
        var future = new FutureResult<CloudPlayer>();
        CloudInstance.instance().client().transmitter().request("player-get", new CommunicationProperty().set("uniqueId", id), CloudPlayerPacket.class, it -> future.complete(it.cloudPlayer()));
        return future.toCompletableFuture();
    }

    @Override
    public CloudPlayer fromPacket(PacketBuffer buffer) {
        var uniqueId = buffer.readUniqueId();
        var name = buffer.readString();
        var currentServer = buffer.readString();
        var currentProxy = buffer.readString();

        return new InstanceCloudPlayerImpl(uniqueId, name, currentServer, currentProxy);
    }
}
