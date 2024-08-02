package dev.httpmarco.polocloud.launcher.dependency;

import dev.httpmarco.polocloud.launcher.PoloCloudLauncher;
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
            PoloCloudLauncher.CLASS_LOADER.addURL(file.toURI().toURL());
            return true;
        }

        Boolean download = DependencyHelper.download(dependency.downloadUrl(), file);

        if(download) {
            PoloCloudLauncher.CLASS_LOADER.addURL(file.toURI().toURL());

            if (dependency.withSubDependencies()) {
                dependency.loadSubDependencies();
                for (var subDependency : dependency.subDependencies()) {
                    download(subDependency);
                }
            }
        }

        return download;
    }


    public void download(Dependency... dependency) {
        for (var depend : dependency) {
            download(depend);
        }
    }
}
