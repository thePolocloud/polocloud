package dev.httpmarco.pololcoud.common.files;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@UtilityClass
public final class FileUtils {

    @SneakyThrows
    private boolean deleteDirectoryContents(@NotNull File directoryPath) {
        if (!directoryPath.exists() || !directoryPath.isDirectory()) {
            return false;
        }

        var files = directoryPath.listFiles();
        if (files == null) {
            return false;
        }

        boolean success = true;
        for (File file : files) {
            success &= deleteRecursively(file);
        }
        Files.delete(directoryPath.toPath());
        return success;
    }

    private boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    if (!deleteRecursively(child)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

    public void delete(@NotNull File file) {
        deleteDirectoryContents(file);
    }

    public void delete(@NotNull Path file) {
        deleteDirectoryContents(file.toFile());
    }

    @SneakyThrows
    public Path createDirectory(Path path) {
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        }
        return path;
    }

    public Path createDirectory(String path) {
        return createDirectory(Path.of(path));
    }

    public boolean copyDirectoryContents(Path sourceDirectoryPath, Path targetDirectoryPath) {
        return copyDirectoryContents(sourceDirectoryPath.toString(), targetDirectoryPath.toString());
    }

    public boolean copyDirectoryContents(String sourceDirectoryPath, String targetDirectoryPath) {
        File sourceDirectory = new File(sourceDirectoryPath);
        File targetDirectory = new File(targetDirectoryPath);

        if (!sourceDirectory.exists() || !sourceDirectory.isDirectory()) {
            return false;
        }

        if (!targetDirectory.exists()) {
            if (!targetDirectory.mkdirs()) {
                return false;
            }
        }

        File[] files = sourceDirectory.listFiles();
        if (files == null) {
            return false;
        }

        boolean success = true;
        for (File file : files) {
            File targetFile = new File(targetDirectory, file.getName());
            if (file.isDirectory()) {
                success &= copyDirectoryContents(file.getAbsolutePath(), targetFile.getAbsolutePath());
            } else {
                success &= copyFile(file.toPath(), targetFile.toPath());
            }
        }
        return success;
    }

    @SneakyThrows
    private boolean copyFile(Path source, Path target) {
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }
}
