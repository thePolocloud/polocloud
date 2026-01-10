package dev.httpmarco.polocloud.runner.expender;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ExpenderElements {

    private final String artifactId;
    private final String groupId;
    private final String version;
    private final File file;
    private final String mainClass;

    public ExpenderElements(String artifactId, String groupId, String version, File file, String mainClass) {
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.version = version;
        this.file = file;
        this.mainClass = mainClass;
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

    public String artifactId() {
        return artifactId;
    }

    public String mainClass() {
        return mainClass;
    }
}
