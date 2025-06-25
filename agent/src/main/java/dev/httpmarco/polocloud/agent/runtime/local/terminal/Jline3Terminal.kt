package dev.httpmarco.polocloud.agent.runtime.local.terminal

import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandService
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl.ClearCommand
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import java.nio.charset.StandardCharsets

class Jline3Terminal {

    private val terminal = TerminalBuilder.builder()
        .system(true)
        .encoding(StandardCharsets.UTF_8)
        .dumb(true)
        .jansi(true)
        .build()
    private val lineReader = LineReaderBuilder.builder()
        .terminal(this.terminal)
        // .completer(new PolocloudTerminalCompleter ())
        .option(LineReader.Option.AUTO_MENU_LIST, true)
        .variable(LineReader.COMPLETION_STYLE_LIST_SELECTION, "fg:cyan")
        .variable(LineReader.COMPLETION_STYLE_LIST_BACKGROUND, "fg:default")
        .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
        .option(LineReader.Option.AUTO_PARAM_SLASH, false)
        .variable(LineReader.BELL_STYLE, "none")
        .build()

    val commandService = CommandService()
    val jLine3Reading = JLine3Reading(this.lineReader, this.commandService)

    init {
        this.commandService.registerCommand(ClearCommand(this))
    }

    fun clearScreen() {
        println("\u001b[H\u001b[2J")
    }

    fun shutdown() {
        this.terminal.close()
    }
}