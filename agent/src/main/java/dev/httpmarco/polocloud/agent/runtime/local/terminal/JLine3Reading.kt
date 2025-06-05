package dev.httpmarco.polocloud.agent.runtime.local.terminal

import org.jline.reader.LineReader

class JLine3Reading(private val lineReader: LineReader) : Thread() {

    override fun run() {

        while (!isInterrupted) {
            var line = lineReader.readLine("polocloud > ");

        }
    }
}