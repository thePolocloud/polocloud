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

import dev.httpmarco.osgan.networking.CommunicationFuture;
import dev.httpmarco.polocloud.api.CloudAPI;
import dev.httpmarco.polocloud.api.events.service.CloudServiceOnlineEvent;
import dev.httpmarco.polocloud.api.groups.CloudGroup;
import dev.httpmarco.polocloud.api.groups.GroupProperties;
import dev.httpmarco.polocloud.api.packets.player.CloudPlayerCountPacket;
import dev.httpmarco.polocloud.api.packets.service.*;
import dev.httpmarco.polocloud.api.services.*;
import dev.httpmarco.polocloud.base.Node;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

@Getter
@Accessors(fluent = true)
public final class CloudServiceProviderImpl extends CloudServiceProvider {

    private final List<CloudService> services = new CopyOnWriteArrayList<>();
    private final CloudServiceFactory factory = new CloudServiceFactoryImpl();
    private final CloudServiceQueue queue = new CloudServiceQueue(this);

    public CloudServiceProviderImpl() {
        // send all services back to request
        var transmitter = Node.instance().transmitter();
        transmitter.responder("services-all", (properties) -> new CloudAllServicesPacket(services));
        transmitter.responder("services-group", (properties) -> new CloudAllServicesPacket(services(CloudAPI.instance().groupProvider().group(properties.getString("name")))));
        transmitter.responder("service-find-id", (properties) -> new CloudServicePacket(find(properties.getUUID("id"))));
        transmitter.responder("service-find-name", (properties) -> new CloudServicePacket(find(properties.getString("name"))));
        transmitter.responder("player-count", (properties) -> new CloudPlayerCountPacket(find(properties.getUUID("id")).onlinePlayersCount()));
        transmitter.responder("services-filtering", property -> new CloudAllServicesPacket(filterService(property.getEnum("filter", ServiceFilter.class))));

        transmitter.listen(CloudServiceShutdownPacket.class, (channel, packet) -> factory.stop(find(packet.uuid())));
        transmitter.listen(CloudServiceStartPacket.class, (channel, packet) -> factory.start(CloudAPI.instance().groupProvider().group(packet.group())));
        transmitter.listen(CloudServiceMaxPlayersUpdatePacket.class, (channel, packet) -> find(packet.id()).maxPlayers(packet.maxPlayers()));

        transmitter.listen(CloudServiceStateChangePacket.class, (channel, packet) -> {
            var service = find(packet.id());
            if (service == null) {
                return;
            }

            if (service instanceof LocalCloudService) {
                ((LocalCloudService) service).state(packet.state());
            } else {
                //todo
            }
            Node.instance().logger().info("The Service &2'&4" + service.name() + "&2' &1is successfully online");
            CloudAPI.instance().globalEventNode().call(new CloudServiceOnlineEvent(service));
        });
        // allow service to start the process
        queue.start();
    }

    public void close() {
        queue.interrupt();
    }

    public void registerService(CloudService cloudService) {
        this.services.add(cloudService);
    }

    public void unregisterService(CloudService cloudService) {
        this.services.remove(cloudService);
    }

    @Override
    public @NotNull CompletableFuture<List<CloudService>> servicesAsync() {
        return CommunicationFuture.completedFuture(this.services);
    }

    @Override
    public @NotNull CompletableFuture<List<CloudService>> filterServiceAsync(@NotNull ServiceFilter filter) {
        return CompletableFuture.completedFuture((switch (filter) {
            case EMPTY_SERVICES -> services.stream().filter(it -> it.onlinePlayersCount() == 0);
            case PLAYERS_PRESENT_SERVERS -> services.stream().filter(it -> it.onlinePlayersCount() > 0);
            case FULL_SERVICES -> services.stream().filter(CloudService::isFull);
            case SAME_NODE_SERVICES -> new ArrayList<CloudService>().stream(); // todo
            case FALLBACKS -> fallbackStream();
            case PROXIES -> services.stream().filter(this::isProxy);
            case SERVERS -> services.stream().filter(it -> !isProxy(it));
            case LOWEST_FALLBACK ->
                    fallbackStream().min(Comparator.comparingInt(CloudService::onlinePlayersCount)).stream();
        }).toList());
    }

    @Override
    public @NotNull CompletableFuture<List<CloudService>> servicesAsync(String group) {
        return CompletableFuture.completedFuture(this.services.stream().filter(it -> it.group().name().equals(group)).toList());
    }

    @Override
    public @NotNull CompletableFuture<CloudService> findAsync(String name) {
        return CompletableFuture.completedFuture(this.services.stream().filter(it -> it.name().equals(name)).findFirst().orElse(null));
    }

    @Override
    public @NotNull CompletableFuture<CloudService> findAsync(UUID id) {
        return CompletableFuture.completedFuture(this.services.stream().filter(it -> it.id().equals(id)).findFirst().orElse(null));
    }

    @Override
    public CloudService generateService(CloudGroup parent, int orderedId, UUID id, int port, ServiceState state, String hostname, int maxMemory, int maxPlayers, String node) {
        //todo
        return null;
    }

    private Stream<CloudService> fallbackStream() {
        return services.stream().filter(it -> !isProxy(it) && it.group().properties().has(GroupProperties.FALLBACK));
    }


    private boolean isProxy(@NotNull CloudService service) {
        return service.group().platform().proxy();
    }
}
