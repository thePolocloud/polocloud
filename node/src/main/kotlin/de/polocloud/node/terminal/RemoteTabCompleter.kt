package de.polocloud.node.terminal

import kotlinx.coroutines.runBlocking
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

/**
 * JLine [Completer] used while a `service <name> screen` session is active.
 *
 * Forwards the whole input line to [TabCompleteBridge], addressed at [serviceName], and
 * offers back whatever suggestions the running service answers with — empty, and so no
 * candidates at all, for a platform that doesn't support tab completion.
 */
class RemoteTabCompleter(private val serviceName: String) : Completer {

    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        val suggestions = runBlocking { TabCompleteBridge.requestCompletions(serviceName, line.line()) }
        suggestions.forEach { candidates.add(Candidate(it)) }
    }
}