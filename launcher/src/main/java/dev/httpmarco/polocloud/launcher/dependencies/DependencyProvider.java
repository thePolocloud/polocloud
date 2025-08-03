package dev.httpmarco.polocloud.launcher.dependencies;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.httpmarco.polocloud.launcher.PolocloudParameters;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

@Accessors(fluent = true)
public final class DependencyProvider {

    private static final Gson GSON = new GsonBuilder().create();

    @Getter
    private final List<Dependency> dependencies;

    @SneakyThrows
    public DependencyProvider() {
        Files.createDirectories(PolocloudParameters.DEPENDENCY_DIRECTORY);

        this.dependencies = List.of(loadDependencyList());
    }

    public void download() {
        this.dependencies.stream().parallel().forEach(Dependency::download);
    }

    @SneakyThrows
    private Dependency[] loadDependencyList() {
        var inputStream = getClass().getResourceAsStream("/dependencies.json");

        if (inputStream == null) {
            throw new RuntimeException("Dependency list not found in classpath");
        }

        var json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        return GSON.fromJson(json, Dependency[].class);
    }
}
