package dev.httpmarco.polocloud.launcher.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@UtilityClass
public final class FileSystemUtils {

    @SneakyThrows
    public void copyClassPathFile(@NotNull ClassLoader classLoader, String fileName, String path) {
        Path target = Path.of(path);

        // create dir if not exists
        target.getParent().toFile().mkdirs();

        Files.copy(Objects.requireNonNull(classLoader.getResourceAsStream(fileName)), target, StandardCopyOption.REPLACE_EXISTING);
    }
}
