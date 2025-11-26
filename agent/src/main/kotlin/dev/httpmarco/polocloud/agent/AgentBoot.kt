package dev.httpmarco.polocloud.agent

import dev.httpmarco.polocloud.agent.logging.LoggingAgent
import dev.httpmarco.polocloud.agent.logging.LoggingLayout
import org.apache.logging.log4j.Logger
import java.lang.instrument.Instrumentation

fun main(args: Array<String>) {
    // try to clean the screen before starting the agent
    println("\u001b[H\u001b[2J")
    // Not work always, but it is a good try

    // save boot time
    System.setProperty("polocloud.lifecycle.boot-time", System.currentTimeMillis().toString())

    // register a clean hook for good shutdown
    registerHook()

    Thread.currentThread().uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, throwable ->
        // todo fix logging here -> bad logs are printed with the logger
        throwable.printStackTrace()
    }
    Agent
}

fun initLogging(debugMode: Boolean = false): Logger {
    val ctx = org.apache.logging.log4j.core.LoggerContext.getContext(false)
    val config = ctx.configuration

    // RootLogger vorbereiten
    val rootLoggerConfig = config.rootLogger

    // Alle alten Appender entfernen
    rootLoggerConfig.appenderRefs.forEach { ref -> rootLoggerConfig.removeAppender(ref.ref) }
    rootLoggerConfig.isAdditive = false

    // Custom Layout und Appender erzeugen
    val layout = LoggingLayout.createLayout()
    val appender = LoggingAgent.create("LoggingAgent", layout)
    appender.start()
    config.addAppender(appender)

    // Root Logger konfigurieren
    rootLoggerConfig.level = if (debugMode) org.apache.logging.log4j.Level.DEBUG else org.apache.logging.log4j.Level.INFO
    rootLoggerConfig.addAppender(appender, rootLoggerConfig.level, null)

    // LoggerContext updaten
    ctx.updateLoggers()

    // Logger zurückgeben
    return org.apache.logging.log4j.LogManager.getLogger("PoloCloud")
}



fun premain(agentArgs: String?, inst: Instrumentation) {

}