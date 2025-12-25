package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.i18n
import dev.httpmarco.polocloud.agent.runtime.Runtime
import dev.httpmarco.polocloud.agent.runtime.abstracts.AbstractServiceStatsThread
import dev.httpmarco.polocloud.agent.runtime.abstracts.AbstractThreadedRuntimeQueue
import dev.httpmarco.polocloud.agent.runtime.local.terminal.JLine3Terminal
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl.GroupCommand
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl.PlatformCommand
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl.PlayersCommand
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl.ServiceCommand
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.impl.TemplateCommand
import dev.httpmarco.polocloud.agent.runtime.local.terminal.setup.impl.OnboardingSetup
import kotlin.io.path.Path
import kotlin.io.path.notExists

class LocalRuntime : Runtime() {

    private val runtimeGroupStorage = LocalRuntimeGroupStorage()
    private val runtimeServiceStorage = LocalRuntimeServiceStorage()
    private val runtimeFactory = LocalRuntimeFactory(this)
    private val runtimeQueue = AbstractThreadedRuntimeQueue()
    private val runtimeExpender = LocalRuntimeExpender()
    private val templates = LocalRuntimeTemplateStorage()
    private val configHolder = LocalRuntimeConfigHolder()

    lateinit var terminal: JLine3Terminal
    private val runtimeCloudInformationThread = LocalCloudInformationThread()

    override fun boot() {
        terminal.commandService.registerCommand(GroupCommand(runtimeGroupStorage, terminal))
        terminal.commandService.registerCommand(ServiceCommand(runtimeServiceStorage, terminal))
        terminal.commandService.registerCommand(PlatformCommand())
        terminal.commandService.registerCommand(TemplateCommand())
        terminal.commandService.registerCommand(PlayersCommand())

        this.runtimeQueue.start()
        this.runtimeCloudInformationThread.start()
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

    override fun serviceStorage() = runtimeServiceStorage

    override fun groupStorage() = runtimeGroupStorage

    override fun factory() = runtimeFactory

    override fun expender() = runtimeExpender

    override fun templateStorage() = templates

    override fun configHolder() = configHolder

    override fun shutdown() {
        this.terminal.shutdown()
        this.runtimeCloudInformationThread.interrupt()
        this.runtimeQueue.interrupt()
        this.runtimeFactory.shutdown()

        i18n.info("agent.shutdown.temp-files.cleanup")
        LOCAL_FACTORY_PATH.toFile().deleteRecursively()
        i18n.info("agent.shutdown.temp-files.cleanup.successful")
    }

    override fun sendCommand(command: String) {
        val tokens = command.split(" ").filter { it.isNotBlank() }
        val commandName = tokens.firstOrNull() ?: return
        val args = tokens.drop(1).toTypedArray()

        this.terminal.commandService.call(commandName, args)
    }

    override fun detectLocalAddress(): String {
        return "127.0.0.1"
    }

    override fun serviceStatsThread(): AbstractServiceStatsThread<*> {
        return LocalServiceStatsThread()
    }
}