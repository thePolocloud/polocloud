package dev.httpmarco.polocloud.agent

import dev.httpmarco.polocloud.agent.configuration.AgentConfig
import dev.httpmarco.polocloud.agent.detector.DetectorFactoryThread
import dev.httpmarco.polocloud.agent.detector.OnlineStateDetector
import dev.httpmarco.polocloud.agent.events.EventService
import dev.httpmarco.polocloud.agent.grpc.GrpcServerEndpoint
import dev.httpmarco.polocloud.agent.i18n.I18nPolocloudAgent
import dev.httpmarco.polocloud.agent.logging.Logger
import dev.httpmarco.polocloud.agent.runtime.Runtime
import dev.httpmarco.polocloud.agent.runtime.local.LocalRuntime
import dev.httpmarco.polocloud.agent.runtime.local.terminal.setup.impl.OnboardingSetup
import dev.httpmarco.polocloud.agent.security.SecurityProvider
import dev.httpmarco.polocloud.platforms.PlatformPool
import kotlin.io.path.Path
import kotlin.io.path.notExists

// global terminal instance for the agent
// this is used to print messages to the console
val logger = Logger()
val developmentMode = System.getProperty("polocloud.version", "false").toBoolean()
val i18n = I18nPolocloudAgent()

object Agent {

    val runtime: Runtime
    val eventService = EventService()
    val securityProvider = SecurityProvider()

    lateinit var config: AgentConfig

    private val grpcServerEndpoint = GrpcServerEndpoint()
    private val onlineStateDetector = DetectorFactoryThread.bindDetector(OnlineStateDetector())

    init {
        // display the default log information
        i18n.info("agent.starting", polocloudVersion())

        if (polocloudVersion().endsWith("-SNAPSHOT")) {
            i18n.warn("agent.version.warn")
        }

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

        this.grpcServerEndpoint.connect(this.config.port)

        this.runtime.boot()

        val groups = runtime.groupStorage().items()

        i18n.info("agent.starting.runtime", runtime::class.simpleName)
        i18n.info("agent.starting.groups.count", groups.size, groups.joinToString(separator = "&8, &7") { it.data.name })
        i18n.info("agent.starting.platforms.count", PlatformPool.size(), PlatformPool.versionSize())
        i18n.info("agent.starting.successful")

        this.onlineStateDetector.detect()
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
}