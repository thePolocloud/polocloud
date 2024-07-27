package dev.httpmarco.polocloud.node.util;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import dev.httpmarco.osgan.networking.CommunicationFuture;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@UtilityClass
public class Downloader {

    private final ExecutorService DOWNLOAD_TASK = Executors.newFixedThreadPool(4);

    public CommunicationFuture<File> downloadAsync(String link, Path path) {
        var future = new CommunicationFuture<File>();

        DOWNLOAD_TASK.submit(() -> {
            try {
                Files.copy(urlStream(link), path);
                future.complete(path.toFile());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    @SneakyThrows
    public File download(String link, Path path) {
        Files.copy(urlStream(link), path);
        return path.toFile();
    }

    public String directJsonDownloader(String link) {
        return JsonUtils.GSON.fromJson(new JsonReader(new InputStreamReader(urlStream(link))), JsonObject.class);
    }

    @SneakyThrows
    private InputStream urlStream(String link) {
        return URI.create(link).toURL().openConnection().getInputStream();
    }
}
