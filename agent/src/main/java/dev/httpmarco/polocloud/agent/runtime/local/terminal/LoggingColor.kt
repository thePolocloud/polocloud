package dev.httpmarco.polocloud.agent.runtime.local.terminal

import org.jline.jansi.Ansi


enum class LoggingColor(private val code: String, private val color: Ansi.Color?, private val bright: Boolean = false) {

    BLACK("&0", Ansi.Color.BLACK),
    DARK_BLUE("&1", Ansi.Color.BLUE),
    DARK_GREEN("&2", Ansi.Color.GREEN),
    DARK_AQUA("&3", Ansi.Color.CYAN),
    DARK_RED("&4", Ansi.Color.RED),
    DARK_PURPLE("&5", Ansi.Color.MAGENTA),
    GOLD("&6", Ansi.Color.YELLOW),
    GRAY("&7", Ansi.Color.WHITE),
    DARK_GRAY("&8", Ansi.Color.BLACK, true),
    BLUE("&9", Ansi.Color.BLUE, true),
    GREEN("&a", Ansi.Color.GREEN, true),
    AQUA("&b", Ansi.Color.CYAN, true),
    RED("&c", Ansi.Color.RED, true),
    LIGHT_PURPLE("&d", Ansi.Color.MAGENTA, true),
    YELLOW("&e", Ansi.Color.YELLOW, true),
    WHITE("&f", Ansi.Color.WHITE, true),
    RESET("&r", null);

    companion object {
        fun translate(message: String): String {
            var message = message
            for (color in entries) {
                message = message.replace(color.code, color.ansi())
            }
            return message + RESET.ansi()
        }
    }

    fun ansi(): String {
        if (color == null) {
            return Ansi.ansi().reset().toString()
        }
        return if (bright) Ansi.ansi().fgBright(color).toString() else Ansi.ansi().fg(color).toString()
    }

    override fun toString(): String {
        return this.ansi()
    }
}