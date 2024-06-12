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
import dev.httpmarco.osgan.files.OsganFile;
import dev.httpmarco.osgan.files.OsganFileCreateOption;
import dev.httpmarco.polocloud.api.groups.CloudGroup;
import dev.httpmarco.polocloud.api.groups.GroupProperties;
import dev.httpmarco.polocloud.api.properties.PropertiesPool;
import dev.httpmarco.polocloud.base.common.PropertiesPoolSerializer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public final class CloudGroupServiceTypeAdapter implements JsonSerializer<CloudGroup>, JsonDeserializer<CloudGroup> {

    private static final Path GROUP_FOLDER = OsganFile.define("local/groups", OsganFileCreateOption.CREATION).path();
    private final Gson LOADER = new GsonBuilder().setPrettyPrinting().serializeNulls()
            .registerTypeHierarchyAdapter(CloudGroup.class, this)
            .registerTypeAdapter(PropertiesPool.class, new PropertiesPoolSerializer())
            .registerTypeHierarchyAdapter(PropertiesPool.class, new PropertiesPoolSerializer())
            .create();

    private final CloudGroupPlatformService platformService;

    @SneakyThrows
    public void includeFile(CloudGroup cloudGroup) {
        Files.writeString(GROUP_FOLDER.resolve(cloudGroup.name() + ".json"), LOADER.toJson(cloudGroup));
    }

    @SneakyThrows
    public void excludeFile(CloudGroup cloudGroup) {
        java.nio.file.Files.delete(GROUP_FOLDER.resolve(cloudGroup.name() + ".json"));
    }

    public void updateFile(CloudGroup cloudGroup) {
        this.includeFile(cloudGroup);
    }

    @SneakyThrows
    public List<CloudGroup> readGroups() {
        var groups = new ArrayList<CloudGroup>();
        for (var file : Objects.requireNonNull(GROUP_FOLDER.toFile().listFiles())) {

            if (!(file.isFile() && file.getName().endsWith(".json"))) {
                continue;
            }

            groups.add(LOADER.fromJson(Files.readString(file.toPath()), CloudGroup.class));
        }
        return groups;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CloudGroup deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        var elements = jsonElement.getAsJsonObject();

        var name = elements.get("name").getAsString();
        var platform = elements.get("platform").getAsString();
        var memory = elements.get("memory").getAsInt();
        var minOnlineServices = elements.get("minOnlineCount").getAsInt();
        var properties = (PropertiesPool<GroupProperties<?>>) jsonDeserializationContext.deserialize(elements.get("properties"), PropertiesPool.class);

        var parentPlatform = platformService.find(platform).possibleVersions().stream().filter(it -> it.version().equals(platform)).findFirst().orElseThrow();

        return new CloudGroupImpl(name, parentPlatform, memory, minOnlineServices, (PropertiesPool<GroupProperties<?>>) properties);
    }

    @Override
    public JsonElement serialize(CloudGroup cloudGroup, Type type, JsonSerializationContext jsonSerializationContext) {
        var object = new JsonObject();

        object.addProperty("name", cloudGroup.name());
        object.addProperty("platform", cloudGroup.platform().version());
        object.addProperty("memory", cloudGroup.memory());
        object.addProperty("minOnlineCount", cloudGroup.minOnlineServices());

        object.add("properties", jsonSerializationContext.serialize(cloudGroup.properties()));

        return object;
    }
}
