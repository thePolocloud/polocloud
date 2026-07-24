package de.polocloud.node.terminal

import de.polocloud.common.commands.CommandService
import de.polocloud.node.core.context.NodeRuntimeContext
import de.polocloud.node.terminal.impl.ClearCommand
import de.polocloud.node.terminal.impl.ClusterCommand
import de.polocloud.node.terminal.impl.GroupCommand
import de.polocloud.node.terminal.impl.InfoCommand
import de.polocloud.node.terminal.impl.PlatformCommand
import de.polocloud.node.terminal.impl.ServiceCommand
import de.polocloud.node.terminal.impl.ShutdownCommand
import de.polocloud.node.terminal.impl.TemplateCommand
import de.polocloud.node.terminal.impl.UpdateCommand
import de.polocloud.node.services.factory.platform.custom.CustomPlatformService
import org.jline.jansi.Ansi
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.Widget
import org.jline.reader.impl.LineReaderImpl
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp
import java.nio.charset.StandardCharsets

/**
 * Wraps a JLine 3 terminal and provides a high-level API for displaying output,
 * managing the input prompt, and coordinating command reading.
 *
 * On creation, the terminal connects to the system console with UTF-8 encoding
 * and configures a [LineReaderImpl] with tab-completion, style options, and
 * sensible defaults for a CLI experience.
 *
 * Use [readingThread] to start the background input loop and [shutdown] to gracefully
 * close the terminal and stop the reading thread.
 */
class CliTerminal(val context: NodeRuntimeContext) : WizardPrompt {

    /**
     * The currently displayed prompt string (ANSI-translated).
     */
    var prompt: String? = AnsiColors.translate("&bpolocloud&8@&7${context.localNodeContainer.data.name()} &8» &7")
    val commandService = CommandService()

    private val defaultCompleter = CommandCompleter(this.commandService)
    private val delegatingCompleter = DelegatingCompleter(this.defaultCompleter)

    private val terminal = TerminalBuilder.builder()
        .system(true)
        .encoding(StandardCharsets.UTF_8)
        .dumb(true)
        .build()

    private val lineReader: LineReaderImpl = LineReaderBuilder.builder()
        .terminal(this.terminal)
        .completer(this.delegatingCompleter)
        .option(LineReader.Option.AUTO_MENU_LIST, true)
        .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
        .option(LineReader.Option.AUTO_PARAM_SLASH, false)
        .variable(LineReader.COMPLETION_STYLE_LIST_SELECTION, "fg:cyan")
        .variable(LineReader.COMPLETION_STYLE_LIST_BACKGROUND, "fg:default")
        .variable(LineReader.BELL_STYLE, "none")
        .build() as LineReaderImpl

    /**
     * The background thread that reads and dispatches user input.
     */
    val readingThread = ReadingThread(this, this.lineReader, this.commandService)

    // Guards every write to the terminal (prompt redraw + log/output printing) so that
    // concurrent callers (the log appender, service log tailing threads, etc.) can't
    // interleave their escape sequences and corrupt the prompt line.
    private val writeLock = Any()

    // While quiet (e.g. a wizard is running), background log output is held here instead
    // of being printed, so it doesn't interleave with the wizard's questions, then flushed
    // once the wizard ends.
    @Volatile
    private var quiet = false
    private val quietBuffer = mutableListOf<String>()

    init {
        this.commandService.registerCommand(
            GroupCommand(
                this.context.groupService,
                this.context.serviceProvider.platformService,
                this.context.serviceProvider,
                this,
            )
        )
        this.commandService.registerCommand(
            ServiceCommand(this.context.serviceProvider, this)
        )
        this.commandService.registerCommand(
            ClusterCommand(
                this.context.localNodeContainer,
                this.context.groupService,
                this.context.serviceProvider,
            )
        )
        this.commandService.registerCommand(
            InfoCommand(
                this.context.localNodeContainer,
                this.context.holder,
                this.context.groupService,
                this.context.serviceProvider,
            )
        )
        this.commandService.registerCommand(ShutdownCommand())
        this.commandService.registerCommand(ClearCommand(this))
        this.commandService.registerCommand(UpdateCommand())
        this.commandService.registerCommand(TemplateCommand(this.context.groupService))
        this.commandService.registerCommand(
            PlatformCommand(
                this.context.serviceProvider.platformService,
                CustomPlatformService(this.context.serviceProvider.platformService),
                this.context.groupService,
                this,
            )
        )
    }

    /**
     * Clears the entire terminal screen.
     */
    override fun clearScreen() = synchronized(writeLock) {
        this.terminal.puts(InfoCmp.Capability.clear_screen)
        this.terminal.flush()
    }

    /**
     * Prints [message] above the current input line, always immediately — unlike
     * [displayApproved], this never gets held back by [quiet]. It's the path a wizard
     * uses for its own questions, which must stay visible even while quiet is suppressing
     * unrelated background log output.
     */
    override fun display(message: String) = synchronized(writeLock) {
        this.lineReader.printAbove(AnsiColors.translate(message))
        this.updateLocked()
    }

    /**
     * Prints a single blank line above the current input line.
     */
    fun emptyLine() = synchronized(writeLock) {
        this.lineReader.printAbove(" ")
    }

    /**
     * Prints [message] above the current input line without disturbing the prompt. Used
     * by the log appender for background output: while [quiet] is active (e.g. a wizard
     * is running), the message is held in [quietBuffer] instead, so unrelated log lines
     * don't interleave with the wizard's questions; [endQuiet] flushes it afterwards.
     */
    fun displayApproved(message: String) = synchronized(writeLock) {
        if (this.quiet) {
            this.quietBuffer.add(message)
            return@synchronized
        }
        this.lineReader.printAbove(message)
        this.updateLocked()
    }

    /**
     * Suppresses background output (e.g. log lines) so an interactive wizard's questions
     * stay uninterrupted; buffered messages are flushed once [endQuiet] is called.
     */
    override fun beginQuiet() = synchronized(writeLock) {
        this.quiet = true
    }

    /**
     * Resumes background output and flushes anything buffered while quiet.
     */
    override fun endQuiet() = synchronized(writeLock) {
        this.quiet = false
        this.quietBuffer.forEach { this.lineReader.printAbove(it) }
        this.quietBuffer.clear()
        this.updateLocked()
    }

    /**
     * Temporarily swaps the line reader's completer, e.g. so a wizard step can offer its
     * own tab-completion suggestions instead of command completion. Pair with
     * [resetCompleter] once the step (or the wizard) is done.
     */
    override fun setCompleter(completer: Completer) {
        this.delegatingCompleter.delegate = completer
    }

    /**
     * Restores the default command completer, undoing [setCompleter].
     */
    override fun resetCompleter() {
        this.delegatingCompleter.delegate = this.defaultCompleter
    }

    /**
     * Forces the JLine prompt to redraw if the reader is currently active.
     * Called automatically after display operations to keep the UI consistent.
     */
    fun update() = synchronized(writeLock) {
        updateLocked()
    }

    private fun updateLocked() {
        if (this.lineReader.isReading) {
            this.lineReader.callWidget(LineReader.REDRAW_LINE)
            this.lineReader.callWidget(LineReader.REDISPLAY)
        }
    }

    /**
     * Updates the prompt to [prompt] (supports `&x` color codes) and redraws the terminal.
     *
     * @param prompt The new prompt string with optional color codes.
     */
    fun updatePrompt(prompt: String) = synchronized(writeLock) {
        this.prompt = AnsiColors.translate(prompt)
        this.lineReader.setPrompt(this.prompt)
        this.updateLocked()
    }

    /**
     * Clears the current (possibly blank) input line above the prompt in a way that's
     * safe to call concurrently with [display]/[displayApproved].
     */
    fun clearCurrentLine() = synchronized(writeLock) {
        this.terminal.writer().print(
            Ansi.ansi().cursorUpLine().eraseLine().toString() + Ansi.ansi().cursorUp(1).toString()
        )
        this.terminal.writer().flush()
    }

    /**
     * Reads a single line of input using the given [prompt].
     *
     * Used by interactive sub-modes (e.g. `service <name> screen`) that need to
     * block for user input while log output is printed above via [displayApproved].
     * Not lock-guarded on purpose: the call blocks until the user submits a line, and
     * holding [writeLock] would stall concurrent output the whole time.
     *
     * @throws org.jline.reader.UserInterruptException on Ctrl+C, which callers may catch
     *         to leave the sub-mode without terminating the node.
     */
    override fun awaitInput(prompt: String): String = this.lineReader.readLine(AnsiColors.translate(prompt))

    /**
     * Like [awaitInput], but binds the Up/Down arrow keys to [onScrollUp]/[onScrollDown]
     * for the duration of this single read instead of the default history navigation.
     * Used by `service <name> screen` to scroll its log viewport while still reusing the
     * normal line-editing/input machinery for typing commands. Bindings are restored
     * (whatever they were before, or unbound) once the read returns, however it ends.
     */
    fun awaitScreenInput(prompt: String, onScrollUp: () -> Unit, onScrollDown: () -> Unit): String {
        val keyMap = this.lineReader.keyMaps[LineReader.MAIN] ?: return awaitInput(prompt)

        val upKeys = listOf("[A", "OA")
        val downKeys = listOf("[B", "OB")
        val previous = (upKeys + downKeys).associateWith { keyMap.getBound(it) }

        upKeys.forEach { keyMap.bind(Widget { onScrollUp(); true }, it) }
        downKeys.forEach { keyMap.bind(Widget { onScrollDown(); true }, it) }
        try {
            return awaitInput(prompt)
        } finally {
            previous.forEach { (sequence, binding) ->
                if (binding != null) keyMap.bind(binding, sequence) else keyMap.unbind(sequence)
            }
        }
    }

    /**
     * How many terminal rows a live scrollback view like `service <name> screen` should
     * use for its log pane, reserving a few rows below for the spacer/help/prompt lines.
     * Falls back to a sane default if the terminal can't report its size.
     */
    fun viewportHeight(): Int {
        val rows = this.terminal.size.rows
        return if (rows <= 0) 20 else (rows - 4).coerceIn(5, 200)
    }

    /**
     * Terminal width in columns, used by `service <name> screen` to figure out how many
     * physical rows a long log line wraps into. Falls back to a sane default if the
     * terminal can't report its size.
     */
    fun viewportWidth(): Int {
        val columns = this.terminal.size.columns
        return if (columns <= 0) 80 else columns
    }

    /**
     * Prints [lines] as a brand new block — never erasing anything — leaving the cursor right
     * after them. Used by `service <name> screen` to lay down a fresh log window before
     * starting a new read for its footer/prompt; whatever was printed for the *previous* read
     * (its own now-stale log window plus the command the user submitted) is simply left behind
     * as scrollback, like any other terminal transcript.
     */
    fun printLogWindow(lines: List<String>) = synchronized(writeLock) {
        val writer = this.terminal.writer()
        lines.forEach { line -> writer.println(AnsiColors.translate(line)) }
        writer.flush()
    }

    /**
     * Repaints an already-on-screen [previousRows]-row log window in place — e.g. `service
     * <name> screen` reacting to a new log line arriving, or the user scrolling — without
     * disturbing whatever is directly below it (the input row JLine is actively reading, which
     * may already have characters typed into it): saves the cursor (wherever it currently sits
     * within that active input row), erases exactly [previousRows] rows above it, prints the
     * new [lines] (always the same count, so nothing net shifts), then restores the cursor
     * to precisely where it was.
     *
     * Deliberately raw ANSI, not JLine's [LineReader.callWidget]-driven redisplay: this
     * terminal is built with `dumb(true)` (see the constructor), and JLine's dumb-terminal
     * code path can't erase anything — it can only ever append — so routing repaints through
     * it just piles up duplicate, overlapping content instead of replacing it. `dumb(true)`
     * only limits what JLine's own higher-level APIs are willing to emit; it doesn't reflect
     * what the real terminal underneath can actually interpret, so writing raw escape codes
     * directly (as this does) works fine.
     */
    fun redrawLogWindow(lines: List<String>, previousRows: Int) = synchronized(writeLock) {
        val writer = this.terminal.writer()
        writer.print(Ansi.ansi().saveCursorPositionDEC().toString())
        repeat(previousRows) {
            // cursorUp (not cursorUpLine) plus an explicit full-line erase: some terminals
            // don't reset the column on cursor-previous-line, which left stale trailing
            // characters from a longer previous line behind a shorter new one.
            writer.print(Ansi.ansi().cursorUp(1).eraseLine(Ansi.Erase.ALL).toString())
        }
        // cursorUp doesn't reset the column, so without this the first printed line would
        // start wherever the cursor's column happened to be beforehand.
        writer.print("\r")
        lines.forEach { line -> writer.println(AnsiColors.translate(line)) }
        writer.print(Ansi.ansi().restoreCursorPositionDEC().toString())
        writer.flush()
    }

    /**
     * Closes the terminal and interrupts the [readingThread] thread.
     */
    fun shutdown() {
        this.terminal.close()
        this.readingThread.interrupt()
    }
}