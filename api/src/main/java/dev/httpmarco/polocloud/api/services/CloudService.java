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

package dev.httpmarco.polocloud.api.services;

import com.google.gson.JsonObject;
import dev.httpmarco.polocloud.api.CloudAPI;
import dev.httpmarco.polocloud.api.groups.CloudGroup;
import dev.httpmarco.polocloud.api.player.CloudPlayer;
import dev.httpmarco.polocloud.api.properties.PropertyPool;
import lombok.SneakyThrows;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public interface CloudService extends Serializable {

    CloudGroup group();

    default String name() {
        return group().name() + "-" + orderedId();
    }

    int orderedId();

    UUID id();

    int port();

    String hostname();

    ServiceState state();

    List<String> log();

    default void shutdown() {
        CloudAPI.instance().serviceProvider().factory().stop(this);
    }

    default boolean isFull() {
        return onlinePlayersCount() >= maxPlayers();
    }

    @SneakyThrows
    default double currentMemory() {
        return this.currentMemoryAsync().get(5, TimeUnit.SECONDS);
    }

    CompletableFuture<Double> currentMemoryAsync();

    int memory();

    int maxPlayers();

    void maxPlayers(int slots);

    @SneakyThrows
    default int onlinePlayersCount() {
        return this.onlinePlayersCountAsync().get(5, TimeUnit.SECONDS);
    }

    CompletableFuture<Integer> onlinePlayersCountAsync();

    void execute(String command);

    List<CloudPlayer> onlinePlayers();

    PropertyPool properties();

    JsonObject toJsonObject();

}
