package dev.httpmarco.polocloud.rest.user;

import com.google.gson.reflect.TypeToken;
import dev.httpmarco.polocloud.rest.RestAPI;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class UserConfiguration {

    private static final Path CONFIG_PATH = Path.of("./local/modules/Rest-Module/users.json");

    @SneakyThrows
    public UserConfiguration() {
        if (!Files.exists(CONFIG_PATH)) {
            if (!Files.exists(CONFIG_PATH.getParent())) {
                Files.createDirectory(CONFIG_PATH.getParent());
            }
            Files.createFile(CONFIG_PATH);
            Files.writeString(CONFIG_PATH, "[]");
        }
    }

    @SneakyThrows
    public List<User> load() {
        var userListType = new TypeToken<List<User>>() {}.getType();
        return RestAPI.GSON.fromJson(Files.readString(CONFIG_PATH), userListType);
    }

    @SneakyThrows
    public <T> void save(T config) {
        Files.writeString(CONFIG_PATH, RestAPI.GSON.toJson(config));
    }
}
