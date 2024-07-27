package dev.httpmarco.polocloud.node.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

@UtilityClass
public final class StringUtils {

    private static final String ALPHABET = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    @SneakyThrows
    public @NotNull String downloadStringContext(String link) {
        var url = new URI(link).toURL();
        var stream = url.openStream();
        var context = new String(stream.readAllBytes());
        stream.close();
        return context;
    }

    public String randomString(int length) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < length; i++) {
            builder.append(ALPHABET.charAt(RandomUtils.getRandomNumber(ALPHABET.length())));
        }

        return builder.toString();
    }
}
