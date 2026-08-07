package de.polocloud.cli.terminal

import de.polocloud.cli.command.CommandService
import de.polocloud.cli.communication.connection.CliConnectionManager
import de.polocloud.cli.prompt.CliPromptProvider
import de.polocloud.cli.prompt.DefaultCliPromptProvider
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
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
class CliTerminal(
    connectionManager: CliConnectionManager
) {

    val commandService = CommandService(connectionManager)
    private val promptProvider: CliPromptProvider = DefaultCliPromptProvider()

    /**
     * The currently displayed prompt string (ANSI-translated).
     */
    var prompt: String? = null

    private val terminal = TerminalBuilder.builder()
        .system(true)
        .encoding(StandardCharsets.UTF_8)
        .dumb(false)
        .build()

    private val lineReader: LineReaderImpl = LineReaderBuilder.builder()
        .terminal(this.terminal)
        .completer(TabCompleter(this.commandService))
        .option(LineReader.Option.AUTO_MENU_LIST, true)
        .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
        .option(LineReader.Option.AUTO_PARAM_SLASH, false)
        .variable(LineReader.COMPLETION_STYLE_LIST_SELECTION, "fg:cyan")
        .variable(LineReader.COMPLETION_STYLE_LIST_BACKGROUND, "fg:default")
        .variable(LineReader.BELL_STYLE, "none")
        .build() as LineReaderImpl

    init {
        updatePrompt(promptProvider.default())
    }

    /**
     * The background thread that reads and dispatches user input.
     */
    val readingThread = ReadingThread(this, this.lineReader, this.commandService)

    /**
     * Clears the entire terminal screen.
     */
    fun clearScreen() {
        this.terminal.puts(InfoCmp.Capability.clear_screen)
        this.terminal.flush()
    }

    /**
     * Prints [message] to the terminal, moving the cursor to the beginning of
     * the line first to avoid prompt overlap. Triggers a prompt redraw afterwards.
     */
    fun display(message: String) {
        this.terminal.puts(InfoCmp.Capability.carriage_return)
        this.terminal.writer().println(message)
        this.terminal.flush()
        this.update()
    }

    /**
     * Prints a single blank line above the current input line.
     */
    fun emptyLine() {
        this.lineReader.printAbove(" ")
    }

    /**
     * Prompts the user with a yes/no [message] and blocks the calling thread until answered.
     *
     * Only "y" or "yes" (case-insensitive) is treated as confirmation; any other input,
     * including blank input, is treated as a rejection. Intended for guarding destructive
     * actions (e.g. shutdown, disconnect) behind an explicit confirmation step.
     *
     * @param message The confirmation question to display, e.g. `"Are you sure...? (yes/no)"`.
     * @return `true` if the user confirmed, `false` otherwise.
     */
    fun confirm(message: String): Boolean {
        val answer = this.lineReader.readLine(AnsiColors.translate("$message "))
        return answer.trim().equals("y", ignoreCase = true) || answer.trim().equals("yes", ignoreCase = true)
    }

    /**
     * Prints [message] above the current input line without disturbing the prompt.
     * Prefer this over [display] when the reading loop is active.
     */
    fun displayApproved(message: String) {
        this.lineReader.printAbove(message)
        this.update()
    }

    /**
     * Forces the JLine prompt to redraw if the reader is currently active.
     * Called automatically after display operations to keep the UI consistent.
     */
    fun update() {
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
    fun updatePrompt(prompt: String) {
        this.prompt = AnsiColors.translate(prompt)
        this.lineReader.setPrompt(this.prompt)
        this.update()
    }

    fun disconnectPrompt() {
        updatePrompt(promptProvider.disconnected())
    }

    fun connectedPrompt(nodeName: String) {
        updatePrompt(promptProvider.connected(nodeName))
    }

    /**
     * Closes the terminal and interrupts the [readingThread] thread.
     */
    fun shutdown() {
        this.terminal.close()
        this.readingThread.interrupt()
    }
}