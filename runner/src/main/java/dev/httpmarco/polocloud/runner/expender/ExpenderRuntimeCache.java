package dev.httpmarco.polocloud.runner.expender;

import dev.httpmarco.polocloud.runner.PolocloudParameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Utility class for managing the expender runtime cache.
 */
public final class ExpenderRuntimeCache {

    /**
     * Clone all cached expender files to the runtime cache directory.
     */
    public static void migrateCacheFiles() {
        List<ExpenderElements> elements = Expender.scanJarCache();

        for (ExpenderElements element : elements) {
            cloneElement(element);
        }
    }

    /**
     * Clones a single expender element to the runtime cache directory.
     * @param element the expender element to clone
     */
    private static void cloneElement(ExpenderElements element) {
        try {
            Path target = PolocloudParameters.EXPENDER_RUNTIME_CACHE.resolve(element.bindPath());
            Files.createDirectories(target.getParent());
            Files.copy(element.getFile().toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
