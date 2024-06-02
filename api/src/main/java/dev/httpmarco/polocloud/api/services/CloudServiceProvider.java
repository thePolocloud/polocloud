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

import dev.httpmarco.osgan.networking.codec.CodecBuffer;
import dev.httpmarco.polocloud.api.groups.CloudGroup;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CloudServiceProvider {

    CloudServiceFactory factory();

    List<CloudService> services();

    CompletableFuture<List<CloudService>> servicesAsync();

    List<CloudService> filterService(ServiceFilter filter);

    CompletableFuture<List<CloudService>> filterServiceAsync(ServiceFilter filter);

    List<CloudService> services(CloudGroup group);

    CloudService find(UUID id);

    CloudService service(String name);

    CloudService fromPacket(CloudGroup parent, CodecBuffer buffer);


}
