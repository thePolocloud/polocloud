package dev.httpmarco.polocloud.agent

import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory
import java.lang.instrument.Instrumentation

fun main(args: Array<String>) {
    // try to clean the screen before starting the agent
    println("\u001b[H\u001b[2J")
    // Not work always, but it is a good try

    // save boot time
    System.setProperty("polocloud.lifecycle.boot-time", System.currentTimeMillis().toString())

    // register a clean hook for good shutdown
    registerHook()


    // initialize logging early
    val builder = ConfigurationBuilderFactory.newConfigurationBuilder()
    builder.setStatusLevel(org.apache.logging.log4j.Level.ERROR)

    val layout = builder.newLayout("PoloLayout")
    val appender = builder.newAppender("PoloAppender", "PoloAppender").add(layout)

    builder.add(appender)
    builder.add(builder.newRootLogger(org.apache.logging.log4j.Level.INFO).add(builder.newAppenderRef("PoloAppender")))

    Configurator.initialize(builder.build())

    Thread.currentThread().uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, throwable ->
        // todo fix logging here -> bad logs are printed with the logger
        throwable.printStackTrace()
    }

    Agent
}

fun premain(agentArgs: String?, inst: Instrumentation) {

}