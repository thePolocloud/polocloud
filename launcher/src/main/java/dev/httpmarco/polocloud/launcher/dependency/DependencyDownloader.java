package dev.httpmarco.polocloud.launcher.dependency;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@UtilityClass
public class DependencyDownloader {

    private static final Path DOWNLOAD_DIR = Path.of("local/dependencies");
    private static final ExecutorService DOWNLOAD_TASK = Executors.newCachedThreadPool();

    static {
        DOWNLOAD_DIR.toFile().mkdirs();

        Runtime.getRuntime().addShutdownHook(new Thread(DOWNLOAD_TASK::shutdown));
    }

    @SneakyThrows
    public Boolean download(Dependency dependency) {
        var file = DOWNLOAD_DIR.resolve(dependency + ".jar").toFile();

        if (file.exists()) {
            return true;
        }

        return DependencyHelper.download(dependency.downloadUrl(), file);
    }


    public void download(Dependency... dependency) {
        for (var depend : dependency) {
            download(depend);
        }
    }
}
