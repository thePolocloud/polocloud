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

import dev.httpmarco.osgan.networking.CommunicationProperty;
import dev.httpmarco.osgan.networking.packet.PacketBuffer;
import dev.httpmarco.osgan.utils.executers.FutureResult;
import dev.httpmarco.polocloud.api.groups.CloudGroup;
import dev.httpmarco.polocloud.api.packets.service.CloudAllServicesPacket;
import dev.httpmarco.polocloud.api.packets.service.CloudServicePacket;
import dev.httpmarco.polocloud.api.services.*;
import dev.httpmarco.polocloud.runner.CloudInstance;
import lombok.SneakyThrows;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class InstanceServiceProvider implements CloudServiceProvider {
    @Override
    public CloudServiceFactory factory() {
        return null;
    }

    @Override
    @SneakyThrows
    public List<CloudService> services() {
        return servicesAsync().get(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<List<CloudService>> servicesAsync() {
        var future = new FutureResult<List<CloudService>>();
        CloudInstance.instance().client().transmitter().request("services-all", CloudAllServicesPacket.class, it -> future.complete(it.services()));
        return future.toCompletableFuture();
    }

    @Override
    @SneakyThrows
    public List<CloudService> filterService(ServiceFilter filter) {
        return filterServiceAsync(filter).get(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<List<CloudService>> filterServiceAsync(ServiceFilter filter) {
        var future = new FutureResult<List<CloudService>>();
        CloudInstance.instance().client().transmitter().request("services-filtering", new CommunicationProperty().set("filter", filter), CloudAllServicesPacket.class, it -> future.complete(it.services()));
        return future.toCompletableFuture();
    }

    @Override
    public List<CloudService> services(CloudGroup group) {
        return List.of();
    }

    @Override
    @SneakyThrows
    public CloudService find(UUID id) {
        return findAsync(id).get(5, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<CloudService> findAsync(UUID id) {
        var future = new FutureResult<CloudService>();
        CloudInstance.instance().client().transmitter().request("service-find", new CommunicationProperty().set("uuid", id), CloudServicePacket.class, it -> future.complete(it.service()));
        return future.toCompletableFuture();
    }

    @Override
    public CloudService service(String name) {
        //todo
        return this.services().stream().filter(it -> it.name().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    @Override
    public CloudService fromPacket(CloudGroup parent, PacketBuffer buffer) {
        var orderedId = buffer.readInt();
        var id = buffer.readUniqueId();
        var port = buffer.readInt();
        var state = buffer.readEnum(ServiceState.class);
        var hostname = buffer.readString();

        return new InstanceCloudService(orderedId, id, port, hostname, state, parent);
    }
}
