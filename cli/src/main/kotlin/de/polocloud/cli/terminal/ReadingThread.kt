package de.polocloud.cli.terminal

import de.polocloud.cli.command.CommandService
import de.polocloud.cli.communication.middleware.GrpcCallException
import de.polocloud.cli.exitPolocloud
import de.polocloud.cli.logger
import org.jline.jansi.Ansi
import org.jline.reader.LineReader
import org.jline.reader.UserInterruptException

/**
 * Background thread that manages the interactive CLI session lifecycle.
 *
 * This thread continuously reads user input from the terminal and acts as the
 * central interaction coordinator of the CLI: it parses each line and dispatches it to
 * the [CommandService], and maintains prompt state so the console remains visually
 * consistent (e.g. handling blank input cleanup).
 *
 * On user interruption (`Ctrl+C`, [UserInterruptException]) the application
 * exits immediately without performing a clean shutdown. Any other exceptions
 * during input handling or command execution are caught and logged so that
 * the interactive loop can continue running.
 *
 * @param terminal The active [CliTerminal] instance responsible for prompt
 *                 and terminal state handling.
 * @param lineReader The JLine [LineReader] used for interactive input reading.
 * @param commandService The [CommandService] used to execute parsed commands.
 */
class ReadingThread(
    private var terminal: CliTerminal,
    private val lineReader: LineReader,
    private val commandService: CommandService
) : Thread("reading-thread") {

    override fun run() {
        while (!isInterrupted) {
            try {
                val line = lineReader.readLine(this.terminal.prompt).trim()

                if (line.isBlank()) {
                    // we reset the terminal prompt as message -> we have a clean console
                    println(Ansi.ansi().cursorUpLine().eraseLine().toString() + Ansi.ansi().cursorUp(1).toString())
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
            } catch (e: GrpcCallException) {
                // Connection/transport failures are expected (e.g. node unreachable) —
                // surface a concise message instead of the raw gRPC stack trace.
                logger.error(e.message)
            } catch (e: Throwable) {
                logger.error("Command execution exception: ", e)
            }
        }
    }
}