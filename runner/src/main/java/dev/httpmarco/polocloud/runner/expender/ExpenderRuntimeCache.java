package dev.httpmarco.polocloud.runner.expender;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ExpenderRuntimeCache {

    private static final Path RUNTIME_CACHE_DIRECTORY = Path.of(".cache");

    public static void migrateCacheFiles() {
        List<ExpenderElements> elements = Expender.scanJarCache();

        for (ExpenderElements element : elements) {
            System.out.println("Migrating " + element.getGroupId() + ":" + element.getArtifactId() + ":" + element.getVersion());
            cloneElement(element);
        }
    }

    private static void cloneElement(ExpenderElements element) {
        try {
            Path target = RUNTIME_CACHE_DIRECTORY.resolve(element.bindPath());


            Files.createDirectories(target.getParent());
            Files.copy(element.getFile().toPath(), RUNTIME_CACHE_DIRECTORY.resolve(element.bindPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
