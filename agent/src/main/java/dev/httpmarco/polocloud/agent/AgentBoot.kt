package dev.httpmarco.polocloud.agent

import java.lang.instrument.Instrumentation

fun main(args: Array<String>) {

    // try to clean the screen before starting the agent
    println("\u001b[H\u001b[2J")
    // Not work always, but it is a good try

    // register a clean hook for good shutdown
    registerHook()

    Agent
}

fun premain(agentArgs: String?, inst: Instrumentation) {

}