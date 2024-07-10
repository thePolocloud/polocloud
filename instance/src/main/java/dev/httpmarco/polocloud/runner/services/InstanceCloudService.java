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

package dev.httpmarco.polocloud.runner.services;

import com.google.gson.JsonObject;
import dev.httpmarco.osgan.networking.CommunicationProperty;
import dev.httpmarco.polocloud.api.groups.CloudGroup;
import dev.httpmarco.polocloud.api.packets.player.CloudPlayerCountPacket;
import dev.httpmarco.polocloud.api.packets.service.CloudServiceMaxPlayersUpdatePacket;
import dev.httpmarco.polocloud.api.player.CloudPlayer;
import dev.httpmarco.polocloud.api.properties.PropertyPool;
import dev.httpmarco.polocloud.api.services.CloudService;
import dev.httpmarco.polocloud.api.services.ServiceState;
import dev.httpmarco.polocloud.runner.CloudInstance;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public final class InstanceCloudService implements CloudService {

    private final int orderedId;
    private final UUID id;

    private final int port;
    private final String hostname;
    private final int memory;

    private final ServiceState state;

    @Setter
    private CloudGroup group;

    private int maxPlayers;
    private final PropertyPool properties = new PropertyPool();

    @Override
    public List<String> log() {
        //todo
        return List.of();
    }

    @Override
    public CompletableFuture<Double> currentMemoryAsync() {
        //todo
        return null;
    }

    @Override
    public void maxPlayers(int slots) {
        this.maxPlayers = slots;
        CloudInstance.instance().client().transmitter().sendPacket(new CloudServiceMaxPlayersUpdatePacket(id, slots));
    }

    @Override
    public CompletableFuture<Integer> onlinePlayersCountAsync() {
        var future = new CompletableFuture<Integer>();
        CloudInstance.instance().client().transmitter().request("player-count", new CommunicationProperty().set("id", this.id), CloudPlayerCountPacket.class, it -> future.complete(it.amount()));
        return future;
    }

    @Override
    public void execute(String command) {
        //todo
    }

    @Override
    public List<CloudPlayer> onlinePlayers() {
        //todo
        return List.of();
    }

    @Override
    public JsonObject toJsonObject() {
        var jsonObject = new JsonObject();
        var info = new JsonObject();

        info.addProperty("group", this.group.name());
        info.addProperty("orderedId", this.orderedId);
        info.addProperty("state", this.state.name());
        info.addProperty("id", this.id.toString());
        jsonObject.add(name(), info);

        return jsonObject;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CloudService service) {
            return this.name().equals(service.name());
        }
        return false;
    }
}
