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

package dev.httpmarco.polocloud.base.groups;

import com.google.gson.*;
import dev.httpmarco.polocloud.api.groups.CloudGroup;
import dev.httpmarco.polocloud.api.properties.PropertyPool;
import dev.httpmarco.polocloud.base.common.PropertiesPoolSerializer;
import dev.httpmarco.pololcoud.common.files.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
public final class CloudGroupServiceTypeAdapter implements JsonSerializer<CloudGroup>, JsonDeserializer<CloudGroup> {

    private static final Path GROUP_FOLDER = FileUtils.createDirectory("local/groups");
    private final Gson LOADER = new GsonBuilder().setPrettyPrinting().serializeNulls()
            .registerTypeHierarchyAdapter(CloudGroup.class, this)
            .registerTypeAdapter(PropertyPool.class, new PropertiesPoolSerializer())
            .registerTypeHierarchyAdapter(PropertyPool.class, new PropertiesPoolSerializer())
            .create();

    private final CloudGroupPlatformService platformService;

    @SneakyThrows
    public void includeFile(@NotNull CloudGroup cloudGroup) {
        Files.writeString(GROUP_FOLDER.resolve(cloudGroup.name() + ".json"), LOADER.toJson(cloudGroup));
    }

    @SneakyThrows
    public void excludeFile(@NotNull CloudGroup cloudGroup) {
        java.nio.file.Files.delete(GROUP_FOLDER.resolve(cloudGroup.name() + ".json"));
    }

    public void updateFile(CloudGroup cloudGroup) {
        this.includeFile(cloudGroup);
    }

    @SneakyThrows
    public CopyOnWriteArrayList<CloudGroup> readGroups() {
        var groups = new CopyOnWriteArrayList<CloudGroup>();
        for (var file : Objects.requireNonNull(GROUP_FOLDER.toFile().listFiles())) {

            if (!(file.isFile() && file.getName().endsWith(".json"))) {
                continue;
            }

            groups.add(LOADER.fromJson(Files.readString(file.toPath()), CloudGroup.class));
        }
        return groups;
    }

    @Override
    public @NotNull CloudGroup deserialize(@NotNull JsonElement jsonElement, Type type, @NotNull JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        var elements = jsonElement.getAsJsonObject();

        var name = elements.get("name").getAsString();
        var platform = elements.get("platform").getAsString();
        var memory = elements.get("memory").getAsInt();
        var minOnlineServices = elements.get("minOnlineCount").getAsInt();
        var properties = (PropertyPool) jsonDeserializationContext.deserialize(elements.get("properties"), PropertyPool.class);

        var parentPlatform = platformService.find(platform).possibleVersions().stream().filter(it -> it.version().equals(platform)).findFirst().orElseThrow();

        var group = new CloudGroupImpl(name, parentPlatform, memory, minOnlineServices);
        group.properties().pool().putAll(properties.pool());
        return group;
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull CloudGroup cloudGroup, Type type, @NotNull JsonSerializationContext jsonSerializationContext) {
        var object = new JsonObject();

        object.addProperty("name", cloudGroup.name());
        object.addProperty("platform", cloudGroup.platform().version());
        object.addProperty("memory", cloudGroup.memory());
        object.addProperty("minOnlineCount", cloudGroup.minOnlineService());

        object.add("properties", jsonSerializationContext.serialize(cloudGroup.properties()));

        return object;
    }
}
