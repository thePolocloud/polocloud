package dev.httpmarco.polocloud.agent.runtime.local.terminal

import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandService
import org.jline.jansi.Ansi
import org.jline.reader.LineReader
import org.jline.reader.UserInterruptException

class JLine3Reading(private val lineReader: LineReader, private val commandService: CommandService) : Thread() {

    override fun run() {

        while (!isInterrupted) {
            try {
                val line = lineReader.readLine("polocloud > ")

                if (line.isBlank()) {
                    // we reset the terminal prompt as message -> we have a clean console
                    println(Ansi.ansi().cursorUpLine().eraseLine().toString() + Ansi.ansi().cursorUp(1).toString())
                    continue
                }

                val splat: Array<String> = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val commandName = splat[0]
                val args = splat.copyOfRange(1, splat.size)

                commandService.call(commandName, args)
            }catch (ex : UserInterruptException) {
                // pressing Ctrl+C or similar to interrupt reading
                // TODO close
                break
            } catch (e: Exception) {
                // Handle other exceptions that may occur during reading
                println("An error occurred while reading input: ${e.message}")
            }
        }
    }
}