package dev.httpmarco.polocloud.node.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;

@UtilityClass
public class ChecksumCalculator {

    public static @NotNull String checksum(Path filename) throws Exception {
        byte[] buffer = new byte[1024];
        int numRead;
        MessageDigest md = MessageDigest.getInstance("sha256");
        try (InputStream is = new FileInputStream(filename.toString())) {
            do {
                numRead = is.read(buffer);
                if (numRead > 0) {
                    md.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
        }

        byte[] digest = md.digest();
        StringBuilder result = new StringBuilder();
        for (byte b : digest) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}