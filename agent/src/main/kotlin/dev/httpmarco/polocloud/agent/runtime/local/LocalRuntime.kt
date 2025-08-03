package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.runtime.Runtime
import dev.httpmarco.polocloud.agent.runtime.local.terminal.JLine3Terminal
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl.GroupCommand
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl.PlatformCommand
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl.ServiceCommand
import dev.httpmarco.polocloud.agent.runtime.local.terminal.setup.impl.OnboardingSetup
import kotlin.io.path.Path
import kotlin.io.path.notExists

class LocalRuntime : Runtime {

    private val runtimeGroupStorage = LocalRuntimeGroupStorage()
    private val runtimeServiceStorage = LocalRuntimeServiceStorage()
    private val runtimeFactory = LocalRuntimeFactory(this)
    private val runtimeQueue = LocalRuntimeQueue()
    private val runtimeExpender = LocalRuntimeExpender()
    private val templates = LocalRuntimeTemplates()
    private val configHolder = LocalRuntimeConfigHolder()

    lateinit var terminal: JLine3Terminal

    override fun boot() {
        terminal.commandService.registerCommand(GroupCommand(runtimeGroupStorage, terminal))
        terminal.commandService.registerCommand(ServiceCommand(runtimeServiceStorage, terminal))
        terminal.commandService.registerCommand(PlatformCommand())

        this.runtimeQueue.start()
    }

    override fun initialize() {
        terminal = JLine3Terminal()

        this.terminal.jLine3Reading.start()

        if (Path("config.json").notExists()) {
            terminal.setupController.start(OnboardingSetup())
            return
        }
        Agent.boot()
    }

    override fun runnable(): Boolean {
        return true // LocalRuntime is always runnable
    }

    override fun serviceStorage() = runtimeServiceStorage

    override fun groupStorage() = runtimeGroupStorage

    override fun factory() = runtimeFactory

    override fun expender() = runtimeExpender

    override fun templates() = templates

    override fun configHolder() = configHolder

    override fun shutdown() {
        this.terminal.shutdown()

        this.runtimeQueue.interrupt()
    }
}