package de.polocloud.node.terminal

import org.jline.reader.UserInterruptException

/**
 * Drives the interactive `service <name> screen` view: a live-appending log pane with a
 * fixed footer (a spacer, an italic dark-gray help line and the input prompt) pinned right
 * below it via a multi-line prompt, and the Up/Down arrow keys scrolling back through the
 * buffered lines instead of navigating input history. Anything typed that isn't `exit` is
 * handed to [sendCommand] to run in the attached service's own console.
 *
 * Not tied to a local or remote service — [append] is the only way lines get in, so the
 * caller wires it up to either a [de.polocloud.node.services.LocalService] log listener or
 * a gRPC log stream, and [sendCommand] to either [de.polocloud.node.services.LocalService.executeCommand]
 * or the equivalent gRPC call.
 */
class ScreenSession(
    private val terminal: CliTerminal,
    private val label: String,
    private val sendCommand: (String) -> Boolean,
) {

    private companion object {
        /** How many lines of scrollback this session keeps, independent of the source's own buffer. */
        const val CAPACITY = 2000

        /** Rows the footer (blank spacer + help line + prompt) occupies, kept in sync with [run]'s prompt. */
        const val PROMPT_ROWS = 3
    }

    private val lock = Any()
    private val log = ArrayDeque<String>(CAPACITY)

    // 0 = following the live tail; >0 = scrolled up that many lines and paused.
    private var scrollOffset = 0

    // Rows currently occupied on screen by our log content (capped at the viewport height),
    // kept accurate across both plain appends and full [redrawViewport] repaints so either
    // one always knows exactly how much of the screen to erase on the next repaint.
    private var screenRows = 0

    /** Appends a newly arrived log line; only reprinted immediately while following (scrollOffset == 0). */
    fun append(line: String) {
        val shouldPrint = synchronized(lock) {
            pushLocked(line)
            scrollOffset == 0
        }
        if (shouldPrint) printLine(line)
    }

    private fun pushLocked(line: String) {
        if (log.size >= CAPACITY) log.removeFirst()
        log.addLast(line)
    }

    private fun printLine(line: String) {
        terminal.display(line)
        screenRows = (screenRows + 1).coerceAtMost(terminal.viewportHeight())
    }

    /** Blocks, driving the input loop until the user types `exit` or hits Ctrl+C. */
    fun run(initialLines: List<String>) {
        synchronized(lock) { initialLines.forEach { pushLocked(it) } }

        terminal.clearScreen()
        initialLines.forEach { printLine(it) }
        terminal.emptyLine()

        val prompt = listOf(
            "",
            CommandOutput.dim("Type 'exit' to leave the screen — use ↑/↓ to scroll."),
            "&8[screen:$label]&r ",
        ).joinToString("\n")

        try {
            while (true) {
                val input = try {
                    terminal.awaitScreenInput(prompt, ::scrollUp, ::scrollDown)
                } catch (_: UserInterruptException) {
                    break
                }
                val trimmed = input.trim()
                if (trimmed.isEmpty()) continue
                if (trimmed.equals("exit", ignoreCase = true)) break
                sendCommand(trimmed)
            }
        } finally {
            terminal.display(CommandOutput.dim("Left the screen for $label."))
        }
    }

    private fun scrollUp() = scroll(1)
    private fun scrollDown() = scroll(-1)

    private fun scroll(delta: Int) {
        val height = terminal.viewportHeight()
        val view = synchronized(lock) {
            val maxOffset = (log.size - height).coerceAtLeast(0)
            scrollOffset = (scrollOffset + delta).coerceIn(0, maxOffset)
            val end = (log.size - scrollOffset).coerceAtLeast(0)
            val start = (end - height).coerceAtLeast(0)
            log.toList().subList(start, end)
        }
        screenRows = terminal.redrawViewport(view, screenRows + PROMPT_ROWS)
    }
}
