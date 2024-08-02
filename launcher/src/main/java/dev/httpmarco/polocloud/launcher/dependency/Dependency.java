package dev.httpmarco.polocloud.launcher.dependency;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
@RequiredArgsConstructor
public final class Dependency {

    private final String groupId;
    private final String artifactId;
    private final String version;

    private Repository repository = Repository.MAVEN_CENTRAL;
    private final List<Dependency> subDependencies = new ArrayList<>();

    public @NotNull String downloadUrl() {
        return repository.repository().formatted(groupId.replace(".", "/"), artifactId, version, artifactId, version);
    }

    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
        return artifactId + "-" + version;
    }
}