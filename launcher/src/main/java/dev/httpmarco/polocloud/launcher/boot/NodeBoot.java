package dev.httpmarco.polocloud.launcher.boot;

import dev.httpmarco.polocloud.launcher.util.FileSystemUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;

public final class NodeBoot extends AbstractBoot {

    @Override
    public void dependencyLoading() {

    }

    @Override
    public @NotNull File bootFile() {
        var path = Path.of("local/dependencies/polocloud-node.jar");
        FileSystemUtils.copyClassPathFile(this.getClass().getClassLoader(), "polocloud-node.jar", path.toString());
        return path.toFile();
    }
}
