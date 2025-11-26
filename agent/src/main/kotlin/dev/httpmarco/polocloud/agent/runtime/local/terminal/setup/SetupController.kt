package dev.httpmarco.polocloud.agent.runtime.local.terminal.setup

import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.local.terminal.JLine3Terminal

class SetupController(private val terminal: JLine3Terminal) {

    private var displayedSetup: Setup<*>? = null

    fun start(setup: Setup<*>) {
        if (displayedSetup != null) {
            return
        }

        // todo
       // logger.enableLogBuffering()

        this.displayedSetup = setup
        displayedSetup?.start(terminal)
    }

    fun active(): Boolean {
        return displayedSetup != null
    }

    fun currentSetup(): Setup<*>? {
        return displayedSetup
    }

    fun completeCurrentSetup() {
        //todo
      //  logger.flushLogs()
        displayedSetup = null
    }

    fun exit() {
        if (displayedSetup == null) {
            return
        }

        displayedSetup?.stop()
        this.displayedSetup = null
    }
}