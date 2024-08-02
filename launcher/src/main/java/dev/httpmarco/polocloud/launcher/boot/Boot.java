package dev.httpmarco.polocloud.launcher.boot;

import java.io.File;

public interface Boot {

    String mainClass();

    void dependencyLoading();

    File bootFile();

}
