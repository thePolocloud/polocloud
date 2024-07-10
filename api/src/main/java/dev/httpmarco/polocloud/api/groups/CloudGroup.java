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

package dev.httpmarco.polocloud.api.groups;

import com.google.gson.JsonObject;
import dev.httpmarco.polocloud.api.CloudAPI;
import dev.httpmarco.polocloud.api.groups.platforms.PlatformVersion;
import dev.httpmarco.polocloud.api.properties.PropertyPool;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public abstract class CloudGroup {

    private String name;
    private PlatformVersion platform;
    private int memory;
    private int minOnlineService;
    private final PropertyPool properties = new PropertyPool();

    @SneakyThrows
    public int onlineAmount() {
        return this.onlineAmountAsync().get(5, TimeUnit.SECONDS);
    }

    public abstract CompletableFuture<Integer> onlineAmountAsync();

    public void update() {
        CloudAPI.instance().groupProvider().update(this);
    }

    public JsonObject toJsonObject() {
        var jsonObject = new JsonObject();
        var info = new JsonObject();
        var platformInfo = new JsonObject();

        platformInfo.addProperty("proxy", this.platform.proxy());

        info.add("platform", platformInfo);
        info.addProperty("memory", this.memory);
        info.addProperty("minOnlineService", this.minOnlineService);

        jsonObject.add(this.name, info);

        return jsonObject;
    }
}
