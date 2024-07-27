package dev.httpmarco.polocloud.node.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.nio.file.Files;
import java.nio.file.Path;

@UtilityClass
public class Configurations {

    @SneakyThrows
    public void writeContent(Path path, Object value) {
        Files.writeString(path, JsonUtils.GSON.toJson(value));
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T> T readContent(Path path, T result) {
        if (!Files.exists(path)) {
            writeContent(path, result);
        }
        return (T) JsonUtils.GSON.fromJson(Files.readString(path), result.getClass());
    }
}
