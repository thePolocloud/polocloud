package dev.httpmarco.polocloud.node.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.httpmarco.polocloud.api.config.ConfigProvider;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ConfigProviderImpl implements ConfigProvider {
    private final Path configDir;
    private final Gson gson;

    @SneakyThrows
    public ConfigProviderImpl() {
        this.configDir = Paths.get("configs");
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        if (Files.notExists(configDir)) {
            Files.createDirectories(configDir);
        }
    }

    @SneakyThrows
    @Override
    public <T> void createConfig(String fileName, T defaultValue) {
        var path = this.configDir.resolve(fileName);

        if (Files.exists(path)) {
            return;
        }

        Files.writeString(path, this.gson.toJson(defaultValue));
    }

    @SneakyThrows
    @Override
    public <T> T readConfig(String fileName, Class<T> tClass) {
        var path = this.configDir.resolve(fileName);

        if (Files.notExists(path)) {
            return null;
        }

        return this.gson.fromJson(Files.readString(path), tClass);
    }

    @Override
    public <T> T readConfigOrCreate(String fileName, Class<T> tClass, T defaultValue) {
        var path = this.configDir.resolve(fileName);

        if (Files.exists(path)) {
            return this.readConfig(fileName, tClass);
        } else {
            this.createConfig(fileName, defaultValue);

            return defaultValue;
        }
    }

    @Override
    public boolean configExists(String fileName) {
        var path = this.configDir.resolve(fileName);

        return Files.exists(path);
    }
}
