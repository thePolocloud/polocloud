package dev.httpmarco.polocloud.base.logging.logback;

public enum AnsiColor {

    RESET("\u001B[0m"),
    BLACK("\u001B[30m"),
    RED("\u001B[31m"),
    GREEN("\u001B[32m"),
    YELLOW("\u001B[33m"),
    BLUE("\u001B[34m"),
    PURPLE("\u001B[35m"),
    CYAN("\u001B[36m"),
    WHITE("\u001B[37m"),
    BRIGHT_BLACK("\u001B[90m");

    public final String ansi;

    AnsiColor(String ansi) {
        this.ansi = ansi;
    }

    /**
     * Strips all Ansi Escape Codes supported by this enum from the string
     *
     * @param coloredString the string to strip
     * @return the string without ansi escape codes
     */
    public static String stripColors(String coloredString) {
        return coloredString.replaceAll("\u001B\\[(0|3[0-7])m", "");
    }

    public static AnsiColor fromName(String name) {
        for (var color : values()) {
            if (color.name().equalsIgnoreCase(name)) {
                return color;
            }
        }
        return null;
    }

    /**
     * Formats a string with the given ansi escape sequence
     *
     * @param stringToColorize the String to be colorized
     * @return the colorized String
     */
    public String colorize(String stringToColorize) {
        return ansi + stringToColorize + RESET.ansi;
    }
}