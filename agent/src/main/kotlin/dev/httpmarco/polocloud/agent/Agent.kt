package dev.httpmarco.polocloud.agent

import dev.httpmarco.polocloud.agent.configuration.AgentConfig
import dev.httpmarco.polocloud.agent.detector.DetectorFactoryThread
import dev.httpmarco.polocloud.agent.detector.OnlineStateDetector
import dev.httpmarco.polocloud.agent.events.EventService
import dev.httpmarco.polocloud.agent.grpc.GrpcServerEndpoint
import dev.httpmarco.polocloud.agent.i18n.I18nPolocloudAgent
import dev.httpmarco.polocloud.agent.module.ModuleProvider
import dev.httpmarco.polocloud.agent.player.PlayerListener
import dev.httpmarco.polocloud.agent.player.PlayerStorageImpl
import dev.httpmarco.polocloud.agent.runtime.Runtime
import dev.httpmarco.polocloud.agent.runtime.local.LocalRuntime
import dev.httpmarco.polocloud.agent.security.SecurityProvider
import dev.httpmarco.polocloud.common.version.polocloudVersion
import dev.httpmarco.polocloud.platforms.PlatformPool
import dev.httpmarco.polocloud.shared.PolocloudShared
import dev.httpmarco.polocloud.shared.events.SharedEventProvider
import dev.httpmarco.polocloud.shared.groups.SharedGroupProvider
import dev.httpmarco.polocloud.shared.player.SharedPlayerProvider
import dev.httpmarco.polocloud.shared.service.SharedServiceProvider
import dev.httpmarco.polocloud.shared.information.SharedCloudInformationProvider
import dev.httpmarco.polocloud.agent.information.CloudInformationStorageImpl
import dev.httpmarco.polocloud.agent.platform.PlatformStorageImpl
import dev.httpmarco.polocloud.shared.platform.SharedPlatformProvider
import dev.httpmarco.polocloud.shared.template.SharedTemplateProvider
import dev.httpmarco.polocloud.updater.Updater
import org.apache.logging.log4j.LogManager

// global terminal instance for the agent
// this is used to print messages to the console
var logger = initLogging()
val i18n = I18nPolocloudAgent()

object Agent : PolocloudShared(true) {

    val runtime: Runtime
    val eventService = EventService()
    val securityProvider = SecurityProvider()
    val moduleProvider = ModuleProvider()

    lateinit var config: AgentConfig

    private val grpcServerEndpoint = GrpcServerEndpoint()
    private val onlineStateDetector = DetectorFactoryThread.bindDetector(OnlineStateDetector())

    val playerStorage = PlayerStorageImpl()
    val cloudInformationStorage = CloudInformationStorageImpl()
    val platformStorage = PlatformStorageImpl()

    init {
        // display the default log information
        i18n.info("agent.starting", polocloudVersion())

        if (polocloudVersion().endsWith("-SNAPSHOT")) {
            i18n.warn("agent.version.warn")
        }

        if (!Updater.hasSyncGitHubVersion()) {
            i18n.warn("agent.version.unavailable")
        }

        this.checkForUpdates()
        this.runtime = Runtime.create()
        this.runtime.initialize()
    }

    /**
     * The boot method is called to start the agent.
     * Its seperated from the constructor to allow for an onboarding setup.
     * The context must be fully initialized before calling this method.
     */
    fun boot() {
        // read all information about the runtime config
        // this is done before the runtime is initialized
        this.config = this.runtime.configHolder().read("config", AgentConfig())

        if (config.autoUpdate && Updater.newVersionAvailable()) {

            if (this.runtime is LocalRuntime) {
                this.runtime.terminal.clearScreen()
            }

            exitPolocloud(cleanShutdown = true, shouldUpdate = true)
            return
        }

        this.moduleProvider.loadModules()

        this.grpcServerEndpoint.connect(this.config.port)

        this.runtime.prepareBoot()

        val groups = runtime.groupStorage().findAll()

        i18n.info("agent.starting.runtime", runtime::class.simpleName)
        i18n.info(
            "agent.starting.groups.count",
            groups.size,
            groups.joinToString(separator = "&8, &7") { it.name })
        i18n.info("agent.starting.platforms.count", PlatformPool.size(), PlatformPool.versionSize())
        i18n.info("agent.starting.successful")

        this.onlineStateDetector.detect()
        PlayerListener()
    }

    /**
     * Close the agent and all its resources.
     * This method will shut down the runtime, close the gRPC server endpoint,
     * and close the online state detector.
     */
    fun close() {
        this.runtime.shutdown()
        this.grpcServerEndpoint.close()
        this.onlineStateDetector.close()
    }

    /**
     * This method check for updates of the agent.
     * If a new version is available, it will log the information.
     */
    fun checkForUpdates() {
        if (Updater.newVersionAvailable()) {
            logger.info("A new version of the agent is available: ${Updater.latestVersion()}")
            return
        }
        logger.info("You are running the latest version of the agent.")
    }

    override fun eventProvider() = this.eventService

    override fun serviceProvider() = this.runtime.serviceStorage()

    override fun groupProvider() = this.runtime.groupStorage()

    override fun playerProvider() = this.playerStorage

    override fun cloudInformationProvider() = this.cloudInformationStorage

    override fun platformProvider() = this.platformStorage

    override fun templateProvider() = this.runtime.templateStorage()
}