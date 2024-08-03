package dev.httpmarco.polocloud.node.platforms;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public interface PlatformPatcher {

    String patchId();

    CompletableFuture<Void> patch(File file);

}
