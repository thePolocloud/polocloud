package dev.httpmarco.polocloud.node.util;

import lombok.experimental.UtilityClass;

import java.util.Random;

@UtilityClass
public class RandomUtils {

    private final Random RANDOM = new Random();

    public int getRandomNumber(int max) {
        return RANDOM.nextInt(max);
    }
}
