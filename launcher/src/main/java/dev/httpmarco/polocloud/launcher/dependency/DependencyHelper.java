package dev.httpmarco.polocloud.launcher.dependency;

import lombok.experimental.UtilityClass;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

@UtilityClass
public class DependencyHelper {

    public void download(String url, File file) {
        try (var inputStream = new URL(url).openStream();
             var outputStream = new BufferedOutputStream(new FileOutputStream(file.toString()))) {

            var buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
