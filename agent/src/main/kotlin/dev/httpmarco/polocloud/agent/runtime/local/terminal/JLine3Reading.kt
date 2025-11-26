package dev.httpmarco.polocloud.agent.runtime.local.terminal

import dev.httpmarco.polocloud.agent.exitPolocloud
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandService
import org.jline.jansi.Ansi
import org.jline.reader.LineReader
import org.jline.reader.UserInterruptException

class JLine3Reading(
    private var terminal: JLine3Terminal,
    private val lineReader: LineReader,
    private val commandService: CommandService
) : Thread() {

    override fun run() {
        this.terminal.resetPrompt()

        while (!isInterrupted) {
            try {
                val line = lineReader.readLine(this.terminal.prompt).trim()

                val screenService = terminal.screenService
                val setupController = terminal.setupController

                if(setupController.active()) {
                    setupController.currentSetup()!!.acceptAnswer(line)
                    continue
                }

                if (line.isBlank()) {
                    // we reset the terminal prompt as message -> we have a clean console
                    println(Ansi.ansi().cursorUpLine().eraseLine().toString() + Ansi.ansi().cursorUp(1).toString())
                    continue
                }

                if (screenService.isRecording()) {
                    if (line == "exit") {
                        screenService.stopCurrentRecording()
                        continue
                    }
                    screenService.redirectCommand(line)
                    continue
                }

                val tokens = line.split(" ").filter { it.isNotBlank() }
                val commandName = tokens.firstOrNull() ?: continue
                val args = tokens.drop(1).toTypedArray()

                commandService.call(commandName, args)
            } catch (_: UserInterruptException) {
                // pressing Ctrl+C or similar to interrupt reading
                exitPolocloud(cleanShutdown = false)
                break
            } catch (e: Throwable) {
                logger.error(e)
            }
        }
    }
}