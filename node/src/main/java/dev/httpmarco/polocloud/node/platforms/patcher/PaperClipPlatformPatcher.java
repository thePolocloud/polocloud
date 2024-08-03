package dev.httpmarco.polocloud.node.platforms.patcher;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public final class PaperClipPlatformPatcher extends AbstractPlatformPatcher {

    public PaperClipPlatformPatcher(String patchId) {
        super(patchId);
    }

    @Override
    public @NotNull CompletableFuture<Void> patch(File file) {
        var future = new CompletableFuture<Void>();

        new Thread(() -> {
            try {
                final var process = new ProcessBuilder("java", "-Dpaperclip.patchonly=true", "-jar", file.getName()).directory(file.getParentFile()).start();
                process.waitFor();
                process.destroyForcibly();
                future.complete(null);
            } catch (InterruptedException | IOException e) {
                future.completeExceptionally(e);
                e.printStackTrace();
            }
        }).start();

        return future;
    }
}