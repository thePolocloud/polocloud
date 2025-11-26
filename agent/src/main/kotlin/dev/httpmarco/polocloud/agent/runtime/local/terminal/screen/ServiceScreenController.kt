package dev.httpmarco.polocloud.agent.runtime.local.terminal.screen

import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.local.terminal.JLine3Terminal
import dev.httpmarco.polocloud.agent.runtime.local.terminal.LoggingColor
import dev.httpmarco.polocloud.agent.services.AbstractService

/**
 * This class is responsible for managing the screen recording of a service.
 * It allows starting and stopping the recording of a service's screen.
 */
class ServiceScreenController(val terminal: JLine3Terminal) {

    private var displayedAbstractService: AbstractService? = null

    fun screenRecordingOf(abstractService: AbstractService) {
        displayedAbstractService = abstractService

        terminal.clearScreen()

        // todo
        //logger.enableLogBuffering()

        abstractService.logs(5000).forEach {
            terminal.display(it)
        }

        terminal.updatePrompt(LoggingColor.translate("&b${abstractService.name()} &8» &7"))
    }

    fun stopCurrentRecording() {

        if (!isRecording()) {
            return
        }

        this.displayedAbstractService = null

        terminal.clearScreen()
        terminal.resetPrompt()

        //todo
       // logger.flushLogs()
    }

    fun isServiceRecoding(abstractService: AbstractService): Boolean {
        return isRecording() && displayedAbstractService!!.name() == abstractService.name()
    }

    fun isRecording(): Boolean {
        return displayedAbstractService != null
    }

    fun redirectCommand(command: String) {
        if (!isRecording()) {
            throw IllegalStateException("Cannot redirect command to service because no service is currently being recorded.")
        }
        this.displayedAbstractService?.executeCommand(command)
    }

    fun displayedAbstractService(): AbstractService? {
        return displayedAbstractService
    }
}