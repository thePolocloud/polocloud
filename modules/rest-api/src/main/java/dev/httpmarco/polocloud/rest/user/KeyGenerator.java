package dev.httpmarco.polocloud.rest.user;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public final class KeyGenerator {

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String PUNCTUATION = "!@#$%&*()_+-=[]|,./?><";
    private final boolean useLower;
    private final boolean useUpper;
    private final boolean useDigits;
    private final boolean usePunctuation;

    private KeyGenerator() {
        throw new UnsupportedOperationException("Empty constructor is not supported.");
    }

    private KeyGenerator(KeyGeneratorBuilder builder) {
        this.useLower = builder.useLower;
        this.useUpper = builder.useUpper;
        this.useDigits = builder.useDigits;
        this.usePunctuation = builder.usePunctuation;
    }

    public static class KeyGeneratorBuilder {

        private boolean useLower;
        private boolean useUpper;
        private boolean useDigits;
        private boolean usePunctuation;

        public KeyGeneratorBuilder() {
            this.useLower = false;
            this.useUpper = false;
            this.useDigits = false;
            this.usePunctuation = false;
        }

        public KeyGeneratorBuilder useLower(boolean useLower) {
            this.useLower = useLower;
            return this;
        }

        public KeyGeneratorBuilder useUpper(boolean useUpper) {
            this.useUpper = useUpper;
            return this;
        }

        public KeyGeneratorBuilder useDigits(boolean useDigits) {
            this.useDigits = useDigits;
            return this;
        }

        public KeyGeneratorBuilder usePunctuation(boolean usePunctuation) {
            this.usePunctuation = usePunctuation;
            return this;
        }

        public KeyGenerator build() {
            return new KeyGenerator(this);
        }
    }

    public String generate(int length) {
        if (length <= 0) {
            return "";
        }

        var password = new StringBuilder(length);
        var random = new SecureRandom();

        List<String> charCategories = new ArrayList<>(4);
        if (this.useLower) {
            charCategories.add(LOWER);
        }
        if (this.useUpper) {
            charCategories.add(UPPER);
        }
        if (this.useDigits) {
            charCategories.add(DIGITS);
        }
        if (this.usePunctuation) {
            charCategories.add(PUNCTUATION);
        }

        for (int i = 0; i < length; i++) {
            var charCategory = charCategories.get(random.nextInt(charCategories.size()));
            int position = random.nextInt(charCategory.length());
            password.append(charCategory.charAt(position));
        }
        return new String(password);
    }
}