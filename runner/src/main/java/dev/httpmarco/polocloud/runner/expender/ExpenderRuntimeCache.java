package dev.httpmarco.polocloud.runner.expender;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class ExpenderRuntimeCache {

    private static final Path RUNTIME_CACHE_DIRECTORY = Paths.get(".cache");

    public static void migrateCacheFiles() {
        List<ExpenderElements> elements = Expender.scanJarCache();

        for (ExpenderElements element : elements) {
            cloneElement(element);
        }
    }

    private static void cloneElement(ExpenderElements element) {
        try {
            Path target = RUNTIME_CACHE_DIRECTORY.resolve(element.bindPath());

            Files.createDirectories(target.getParent());
            Files.copy(element.getFile().toPath(), RUNTIME_CACHE_DIRECTORY.resolve(element.bindPath()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
