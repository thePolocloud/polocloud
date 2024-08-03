package dev.httpmarco.polocloud.launcher.dependency;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public final class Dependency {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private @Nullable String subVersion;

    private Repository repository = Repository.MAVEN_CENTRAL;
    private final List<Dependency> subDependencies = new ArrayList<>();

    public Dependency(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public @NotNull String downloadUrl() {
        return repository.repository().formatted(groupId.replace(".", "/"), artifactId, version, artifactId, (subVersion == null ? version : subVersion));
    }

    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
        return artifactId + "-" + version;
    }
}