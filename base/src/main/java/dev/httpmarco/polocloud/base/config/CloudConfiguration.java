package dev.httpmarco.polocloud.base.config;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class CloudConfiguration {

    private final String javaCommand;
    private final String promt;
    private final String runningDirectory;

    public CloudConfiguration() {
        this.javaCommand = "java";
        this.promt = "&3cloud &2» &1";
        this.runningDirectory = "running/";
    }
}
