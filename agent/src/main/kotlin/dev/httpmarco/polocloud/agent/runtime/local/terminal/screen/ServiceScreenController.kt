package dev.httpmarco.polocloud.agent.runtime.local.terminal.screen

import dev.httpmarco.polocloud.agent.runtime.local.terminal.JLine3Terminal
import dev.httpmarco.polocloud.agent.runtime.local.terminal.LoggingColor
import dev.httpmarco.polocloud.agent.services.Service

/**
 * This class is responsible for managing the screen recording of a service.
 * It allows starting and stopping the recording of a service's screen.
 */
class ServiceScreenController(val terminal: JLine3Terminal) {

    private var displayedService: Service? = null

    fun screenRecordingOf(service: Service) {
        displayedService = service

        terminal.clearScreen()

        service.logs(5000).forEach {
            terminal.display(it)
        }

        terminal.updatePrompt(LoggingColor.translate("&b${service.name()} &8» &7"))
    }

    fun stopCurrentRecording() {

        if (!isRecoding()) {
            return
        }

        this.displayedService = null

        terminal.clearScreen()
        terminal.resetPrompt()
        // todo display the context before the recording
    }

    fun isServiceRecoding(service: Service): Boolean {
        return isRecoding() && displayedService!!.name() == service.name()
    }

    fun isRecoding(): Boolean {
        return displayedService != null
    }

    fun redirectCommand(command: String) {
        if (!isRecoding()) {
            throw IllegalStateException("Cannot redirect command to service because no service is currently being recorded.")
        }
        this.displayedService?.executeCommand(command)
    }
}