package de.polocloud.runner.runtime;

public class RuntimeResolver {

    private static final String PROPERTY = "polocloud.mode";

    private RuntimeResolver() {}

    public static RuntimeMode resolve(String[] args) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--cli")) return RuntimeMode.CLI;
            if (arg.equalsIgnoreCase("--node")) return RuntimeMode.NODE;
            if (arg.equalsIgnoreCase("--prime-cache")) return RuntimeMode.PRIME_CACHE;
        }

        String mode = System.getProperty(PROPERTY);
        if (mode != null && !mode.isEmpty()) {
            return RuntimeMode.valueOf(mode.toUpperCase());
        }

        return RuntimeMode.NODE;
    }
}
