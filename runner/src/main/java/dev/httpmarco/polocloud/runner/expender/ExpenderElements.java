package dev.httpmarco.polocloud.runner.expender;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ExpenderElements {

    private final String artifactId;
    private final String groupId;
    private final String version;
    private final File file;

    public ExpenderElements(String artifactId, String groupId, String version, File file) {
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.version = version;
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public Path bindPath() {
        return Paths.get(
                groupId.replace(".", "/"),
                artifactId,
                version,
                artifactId + "-" + version + ".jar"
        );
    }
}
