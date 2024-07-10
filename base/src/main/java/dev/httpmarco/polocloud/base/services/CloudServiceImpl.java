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

package dev.httpmarco.polocloud.base.services;

import com.google.gson.JsonObject;
import dev.httpmarco.polocloud.api.CloudAPI;
import dev.httpmarco.polocloud.api.groups.CloudGroup;
import dev.httpmarco.polocloud.api.packets.service.CloudServiceMaxPlayersUpdatePacket;
import dev.httpmarco.polocloud.api.player.CloudPlayer;
import dev.httpmarco.polocloud.api.properties.PropertyPool;
import dev.httpmarco.polocloud.api.services.CloudService;
import dev.httpmarco.polocloud.api.services.ServiceState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
@Getter
@Accessors(fluent = true)
public class CloudServiceImpl implements CloudService {

    private CloudGroup group;
    private int orderedId;
    private UUID id;

    private int port;
    private String hostname;

    @Setter(AccessLevel.PACKAGE)
    private ServiceState state;

    private final int memory;
    private int maxPlayers;

    private final PropertyPool properties = new PropertyPool();
    private final Set<String> subscribedEvents = new HashSet<>();

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

        if (this instanceof LocalCloudService cloudService) {
            cloudService.channelTransmit().sendPacket(new CloudServiceMaxPlayersUpdatePacket(id, maxPlayers));
        }
    }

    @Override
    public CompletableFuture<Integer> onlinePlayersCountAsync() {
        return CompletableFuture.completedFuture(onlinePlayers().size());
    }

    @Override
    public void execute(String command) {
        // todo send packet to self hosted node
    }

    @Override
    public List<CloudPlayer> onlinePlayers() {
        return CloudAPI.instance().playerProvider().players().stream().filter(it -> (group.platform().proxy() ? it.currentProxyName() : it.currentServerName()).equalsIgnoreCase(this.name())).toList();
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
    public String toString() {
        return "group=" + group + ", orderedId=" + orderedId + ", state=" + state + ", id=" + id;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CloudService service) {
            return this.name().equals(service.name());
        }
        return false;
    }
}
