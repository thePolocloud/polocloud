package dev.httpmarco.polocloud.launcher.dependency;

import dev.httpmarco.polocloud.launcher.dependency.sub.SubDependencyHelper;
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

    private Repository repository;
    private final boolean withSubDependencies;
    private final List<Dependency> subDependencies = new ArrayList<>();

    public Dependency(String groupId, String artifactId, String version) {
        this(groupId, artifactId, version, null, Repository.MAVEN_CENTRAL, false);
    }

    public Dependency(String groupId, String artifactId, String version, boolean withSubDependencies) {
        this(groupId, artifactId, version, null, Repository.MAVEN_CENTRAL, withSubDependencies);
    }

    public Dependency(String groupId, String artifactId, String version, String subVersion, Repository repository) {
        this(groupId, artifactId, version, subVersion, repository, false);
    }


    public @NotNull String downloadUrl() {
        return repository.repository().formatted(groupId.replace(".", "/"), artifactId, version, artifactId, (subVersion == null ? version : subVersion));
    }

    public void loadSubDependencies() {
        this.subDependencies.addAll(SubDependencyHelper.findSubDependencies(this));
    }

    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
        return artifactId + "-" + version;
    }
}