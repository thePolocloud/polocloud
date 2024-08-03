package dev.httpmarco.polocloud.node.platforms;

import dev.httpmarco.polocloud.node.util.JsonUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Log4j2
@Getter
@Accessors(fluent = true)
public final class PlatformService {

    private final Platform[] platforms;
    private final List<PlatformPatcher> patchers = new ArrayList<>();

    @SneakyThrows
    public PlatformService() {
        var versionFile = Path.of("local/versions.json");

        if (!Files.exists(versionFile.getParent())) {
            Files.createDirectory(versionFile.getParent());
        }

        if (Files.exists(versionFile)) {
            // todo check new update (compare versions id)
        } else {
            this.loadLatestVersionFromClasspath(versionFile);
        }

        this.platforms = this.readLocalPlatformConfig(versionFile).platforms();
        log.info("Loading {} cluster platforms with {} versions&8.", platforms.length, versionsAmount());
    }

    @SneakyThrows
    private PlatformConfig readLocalPlatformConfig(Path versionFile) {
        return JsonUtils.GSON.fromJson(Files.readString(versionFile), PlatformConfig.class);
    }

    public @Nullable Platform platform(String platform) {
        return Arrays.stream(this.platforms).filter(it -> it.platform().equalsIgnoreCase(platform)).findFirst().orElse(null);
    }

    public boolean exists(String platform) {
        return Arrays.stream(this.platforms).anyMatch(it -> it.platform().equalsIgnoreCase(platform));
    }

    @SneakyThrows
    private void loadLatestVersionFromClasspath(Path versionFile) {
        Files.copy(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("versions.json")), versionFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private int versionsAmount() {
        return Arrays.stream(this.platforms).mapToInt(it -> it.versions().size()).sum();
    }
}
