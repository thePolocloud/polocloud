package de.polocloud.node.terminal

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shared value styling for terminal command output (`service`, `group`, `cluster`, `info`,
 * `platform`, `template`), so every command's `info`/`list` view reads the same way:
 * labels in the default color, values wrapped in [white], timestamps in [dim] via [timestamp].
 */
object CommandOutput {

    /** Wraps [text] in white (`&f`) so command-output values stand out from their labels. */
    fun white(text: String): String = "&f$text&r"

    /**
     * Wraps [text] in a dim italic style (raw ANSI italic + `&8` dark gray) used for
     * timestamps, so they read as secondary/meta information rather than a normal value.
     */
    fun dim(text: String): String = "[3m&8$text&r"

    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

    /** Formats [epochMillis] as an absolute timestamp plus a human-readable elapsed duration, dimmed. */
    fun timestamp(epochMillis: Long, elapsedText: (String) -> String): String {
        val formatted = timestampFormatter.format(Instant.ofEpochMilli(epochMillis))
        val elapsed = formatElapsed(System.currentTimeMillis() - epochMillis)
        return dim("$formatted (${elapsedText(elapsed)})")
    }

    fun formatElapsed(millis: Long): String {
        val duration = Duration.ofMillis(millis.coerceAtLeast(0))
        return when {
            duration.toHours() > 0 -> "${duration.toHours()}h ${duration.toMinutesPart()}m"
            duration.toMinutes() > 0 -> "${duration.toMinutes()}m ${duration.toSecondsPart()}s"
            else -> "${duration.toSeconds()}s"
        }
    }

    // Locale.ROOT: a German (or other comma-decimal) system locale would otherwise turn
    // "12.3" into "12,3", which reads as a typo/thousands-separator in the CLI output.
    fun decimal(value: Double): String = String.format(Locale.ROOT, "%.1f", value)
}