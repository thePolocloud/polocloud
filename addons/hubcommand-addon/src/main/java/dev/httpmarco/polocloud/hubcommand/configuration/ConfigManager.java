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

package dev.httpmarco.polocloud.hubcommand.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;

@Getter
public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("./plugins/PoloCloud-HubCommand/config.json");
    private MessagesConfiguration messagesConfiguration;

    @SneakyThrows
    public ConfigManager() {
        if (!Files.exists(CONFIG_PATH)) {
            Files.createDirectory(CONFIG_PATH.getParent());
            this.messagesConfiguration = new MessagesConfiguration();

            save(messagesConfiguration);
        }

        this.messagesConfiguration = load(MessagesConfiguration.class);
    }

    @SneakyThrows
    public static <T> T load(Class<T> clazz) {
        return GSON.fromJson(Files.readString(CONFIG_PATH), clazz);
    }

    @SneakyThrows
    public static <T> void save(T config) {
        Files.writeString(CONFIG_PATH, GSON.toJson(config));
    }

}
