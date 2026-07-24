package de.polocloud.node.terminal

import org.jline.reader.UserInterruptException

/**
 * Drives the interactive `service <name> screen` view: a live-appending log pane with a
 * fixed footer (a spacer, an italic dark-gray help line and the input prompt) pinned right
 * below it, and the Up/Down arrow keys scrolling back through the buffered lines instead of
 * navigating input history. Anything typed that isn't `exit` is handed to [sendCommand] to
 * run in the attached service's own console.
 *
 * The footer is printed exactly once per submitted input line, via JLine's own prompt
 * argument to [CliTerminal.awaitScreenInput] — same as any other command prompt. The log
 * window above it is drawn entirely by hand ([CliTerminal.printLogWindow]/[redrawLogWindow]),
 * deliberately bypassing JLine's `Display`/redisplay machinery: this terminal is built with
 * `dumb(true)`, and JLine's dumb-terminal code path can only ever append, never erase — so
 * routing repaints through it (an earlier attempt at this) just piled up duplicate content
 * instead of replacing it. Raw ANSI cursor movement and line erasure work fine here, since
 * `dumb(true)` only limits what JLine's own APIs are willing to emit, not what the real
 * terminal underneath can interpret.
 *
 * Not tied to a local or remote service — [append] is the only way lines get in, so the
 * caller wires it up to either a [de.polocloud.node.services.LocalService] log listener or
 * a gRPC log stream, and [sendCommand] to either [de.polocloud.node.services.LocalService.executeCommand]
 * or the equivalent gRPC call.
 */
class ScreenSession(
    private val terminal: CliTerminal,
    private val label: String,
    // Distinct from [label] (which may be decorated with "@ node" for a remote screen) —
    // this is the plain service name TabCompleteRequestEvent addresses.
    private val serviceName: String,
    private val sendCommand: (String) -> Boolean,
) {

    private companion object {
        /** How many lines of scrollback this session keeps, independent of the source's own buffer. */
        const val CAPACITY = 2000

        /** Matches the legacy `&x` color codes so they don't count towards a line's on-screen width. */
        val COLOR_CODE_REGEX = Regex("&[0-9a-fr]")
    }

    private val lock = Any()
    private val log = ArrayDeque<String>(CAPACITY)

    // 0 = following the live tail; >0 = scrolled up that many lines and paused.
    private var scrollOffset = 0

    // How many rows the log window currently on screen occupies — always exactly the
    // viewport height once the first window for the active input line has been printed, so
    // [redraw] knows precisely how much to erase without touching the input row below it.
    private var displayedRows = 0

    /** Appends a newly arrived log line; only repainted immediately while following (scrollOffset == 0). */
    fun append(line: String) {
        val shouldRedraw = synchronized(lock) {
            pushLocked(line)
            scrollOffset == 0
        }
        if (shouldRedraw) redraw()
    }

    private fun pushLocked(line: String) {
        if (log.size >= CAPACITY) log.removeFirst()
        log.addLast(line)
    }

    /** Blocks, driving the input loop until the user types `exit` or hits Ctrl+C. */
    fun run(initialLines: List<String>) {
        synchronized(lock) { initialLines.forEach { pushLocked(it) } }
        terminal.clearScreen()

        // While the screen is open, only its own content (the log window + footer below) may
        // appear — everything else the node would normally print in the background is held
        // back until we leave.
        terminal.beginQuiet()
        // Offers whatever completions the attached service's own console would — see
        // RemoteTabCompleter/TabCompleteBridge. Falls back to no candidates at all for a
        // platform that never answers, rather than the unrelated top-level command completer.
        terminal.setCompleter(RemoteTabCompleter(serviceName))
        try {
            while (true) {
                // A fresh block every input line — the previous one (now stale) is simply left
                // behind as scrollback, like any other terminal transcript; only while *this*
                // read is active does [redraw] repaint the window it lays down here in place.
                printFreshWindow()

                val footer = listOf(
                    "",
                    CommandOutput.dim("Type 'exit' to leave the screen — use ↑/↓ to scroll."),
                    "&8[screen:$label]&r ",
                ).joinToString("\n")

                val input = try {
                    terminal.awaitScreenInput(footer, ::scrollUp, ::scrollDown)
                } catch (_: UserInterruptException) {
                    break
                }
                val trimmed = input.trim()
                if (trimmed.isEmpty()) continue
                if (trimmed.equals("exit", ignoreCase = true)) break
                sendCommand(trimmed)
            }
        } finally {
            terminal.resetCompleter()
            terminal.endQuiet()
            terminal.display(CommandOutput.dim("Left the screen for $label."))
        }
    }

    private fun printFreshWindow() {
        val height = terminal.viewportHeight()
        val window = synchronized(lock) { buildWindow(height) }
        terminal.printLogWindow(window)
        displayedRows = height
    }

    private fun redraw() {
        val height = terminal.viewportHeight()
        val window = synchronized(lock) { buildWindow(height) }
        terminal.redrawLogWindow(window, displayedRows)
        displayedRows = height
    }

    /**
     * Selects lines ending at the current scroll position, walking backward until the next
     * one would push the physical row total (accounting for terminal-width wrapping of long
     * lines, see [visualRows]) past [height], then pads with leading blank rows up to exactly
     * [height] — so the window always occupies precisely [height] rows on screen, which is
     * what lets [redraw] erase-and-reprint it without ever needing to touch (or guess the size
     * of) whatever's below. Must be called with [lock] held.
     */
    private fun buildWindow(height: Int): List<String> {
        val end = (log.size - scrollOffset).coerceAtLeast(0)
        var start = end
        var rows = 0
        while (start > 0) {
            val rowsForLine = visualRows(log[start - 1])
            // Always take at least one line, even if it alone wraps past height — otherwise
            // a single very long line would leave the window empty.
            if (rows > 0 && rows + rowsForLine > height) break
            rows += rowsForLine
            start--
        }
        val lines = (start until end).map { log[it] }
        return List((height - rows).coerceAtLeast(0)) { "" } + lines
    }

    /**
     * How many physical terminal rows [line] wraps into once printed — a Minecraft log line
     * (join messages with coordinates, etc.) is often wider than the terminal, and undercounting
     * that here means [redraw]'s erase leaves the wrapped-over remainder on screen.
     */
    private fun visualRows(line: String): Int {
        val width = terminal.viewportWidth().coerceAtLeast(1)
        val length = line.replace(COLOR_CODE_REGEX, "").length
        return if (length <= width) 1 else (length + width - 1) / width
    }

    private fun scrollUp() = scroll(1)
    private fun scrollDown() = scroll(-1)

    private fun scroll(delta: Int) {
        val height = terminal.viewportHeight()
        val changed = synchronized(lock) {
            val maxOffset = (log.size - height).coerceAtLeast(0)
            val newOffset = (scrollOffset + delta).coerceIn(0, maxOffset)
            val changed = newOffset != scrollOffset
            scrollOffset = newOffset
            changed
        }
        if (changed) redraw()
    }
}