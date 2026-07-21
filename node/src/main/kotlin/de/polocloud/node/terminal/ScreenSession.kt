package de.polocloud.node.terminal

import org.jline.reader.UserInterruptException

/**
 * Drives the interactive `service <name> screen` view: a live-appending log pane with a
 * fixed footer (a spacer, an italic dark-gray help line and the input prompt) pinned right
 * below it, and the Up/Down arrow keys scrolling back through the buffered lines instead of
 * navigating input history. Anything typed that isn't `exit` is handed to [sendCommand] to
 * run in the attached service's own console.
 *
 * The whole viewport (visible log window + footer) is rebuilt as a single multi-line string
 * and pushed via [CliTerminal.updateActivePrompt] on every change (a new line arriving, or the
 * user scrolling) — letting JLine's own `Display` own the terminal throughout, which handles
 * erasing stale content and wrapping long lines correctly on its own. Earlier attempts at
 * doing that by hand with raw ANSI cursor/erase codes fought with `Display`'s internal
 * bookkeeping (which assumes it's the sole writer to the terminal and never re-queries it)
 * and drifted further out of sync with every scroll.
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
    }

    private val lock = Any()
    private val log = ArrayDeque<String>(CAPACITY)

    // 0 = following the live tail; >0 = scrolled up that many lines and paused.
    private var scrollOffset = 0

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

        // While the screen is open, only its own content (rendered via the prompt below) may
        // appear — everything else the node would normally print in the background is held
        // back until we leave.
        terminal.beginQuiet()
        // Offers whatever completions the attached service's own console would — see
        // RemoteTabCompleter/TabCompleteBridge. Falls back to no candidates at all for a
        // platform that never answers, rather than the unrelated top-level command completer.
        terminal.setCompleter(RemoteTabCompleter(serviceName))
        try {
            while (true) {
                val input = try {
                    terminal.awaitScreenInput(currentViewport(), ::scrollUp, ::scrollDown)
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

    private fun redraw() = terminal.updateActivePrompt(currentViewport())

    /** The visible log window followed by the fixed footer, as one multi-line prompt string. */
    private fun currentViewport(): String {
        val height = terminal.viewportHeight()
        val view = synchronized(lock) {
            val end = (log.size - scrollOffset).coerceAtLeast(0)
            val start = (end - height).coerceAtLeast(0)
            log.toList().subList(start, end)
        }
        return (view + listOf(
            "",
            CommandOutput.dim("Type 'exit' to leave the screen — use ↑/↓ to scroll."),
            "&8[screen:$label]&r ",
        )).joinToString("\n")
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