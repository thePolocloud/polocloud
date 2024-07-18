package dev.httpmarco.polocloud.rest.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.nio.file.Files;
import java.nio.file.Path;

@Getter
@Accessors(fluent = true)
public class Config {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("./local/modules/Rest-Module/config.json");
    private final JavalinConfiguration javalinConfiguration;

    @SneakyThrows
    public Config() {
        if (!Files.exists(CONFIG_PATH)) {
            if (!Files.exists(CONFIG_PATH.getParent())) {
                Files.createDirectory(CONFIG_PATH.getParent());
            }
            Files.createFile(CONFIG_PATH);
            this.javalinConfiguration = new JavalinConfiguration();

            save(this.javalinConfiguration);
            return;
        }

        this.javalinConfiguration = load(JavalinConfiguration.class);
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
