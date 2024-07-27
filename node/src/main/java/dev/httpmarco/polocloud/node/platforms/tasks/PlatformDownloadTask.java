package dev.httpmarco.polocloud.node.platforms.tasks;

import dev.httpmarco.polocloud.node.platforms.Platform;
import dev.httpmarco.polocloud.node.platforms.PlatformVersion;
import dev.httpmarco.polocloud.node.util.ChecksumCalculator;
import dev.httpmarco.polocloud.node.util.Downloader;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

@Log4j2
@UtilityClass
public final class PlatformDownloadTask {

    private static final Path PLATFORM_DIR = Path.of("local/platforms");

    @SneakyThrows
    public void download(@NotNull Platform platform, @NotNull PlatformVersion version) {
        var versionPath = PLATFORM_DIR.resolve(platform.platform()).resolve(version.version());

        versionPath.toFile().mkdirs();

        var file = versionPath.resolve(platform.platform() + "-" + version.version() + ".jar");

        if (!Files.exists(file)) {
            Downloader.download(version.downloadLink(), file);
        }

        if (!version.checksum().equals(ChecksumCalculator.checksum(file))) {
            // todo delete and redownload
        }
    }
}