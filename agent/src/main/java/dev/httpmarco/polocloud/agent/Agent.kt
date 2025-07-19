package dev.httpmarco.polocloud.agent

import dev.httpmarco.polocloud.agent.detector.DetectorFactoryThread
import dev.httpmarco.polocloud.agent.detector.OnlineStateDetector
import dev.httpmarco.polocloud.agent.events.EventService
import dev.httpmarco.polocloud.agent.grpc.GrpcServerEndpoint
import dev.httpmarco.polocloud.agent.i18n.I18nPolocloudAgent
import dev.httpmarco.polocloud.agent.logging.Logger
import dev.httpmarco.polocloud.agent.runtime.Runtime
import dev.httpmarco.polocloud.agent.security.SecurityProvider
import dev.httpmarco.polocloud.platforms.PlatformPool
import dev.httpmarco.polocloud.platforms.tasks.PlatformTaskPool

// global terminal instance for the agent
// this is used to print messages to the console
val logger = Logger()
val i18n = I18nPolocloudAgent()

class Agent {

    val runtime: Runtime
    val eventService = EventService()
    val securityProvider = SecurityProvider()

    private val grpcServerEndpoint = GrpcServerEndpoint()
    private val onlineStateDetector = DetectorFactoryThread.bindDetector(OnlineStateDetector())

    companion object {
        val instance = Agent()
    }

    init {
        // display the default log information
        i18n.info("agent.starting")
        this.runtime = Runtime.create()
        this.grpcServerEndpoint.connect()

        i18n.info("agent.starting.runtime", runtime::class.simpleName)

        val groups = runtime.groupStorage().items()
        i18n.info("agent.starting.groups.count", groups.size, groups.joinToString(separator = ", ") { it.data.name })
        i18n.info("agent.starting.platforms.count", PlatformPool.size(), PlatformTaskPool.size())

        i18n.info("agent.starting.successful")

        this.onlineStateDetector.detect()
        this.runtime.postInitialize()
    }

    fun close() {
        this.runtime.shutdown()
        this.grpcServerEndpoint.close()
        this.onlineStateDetector.close()
    }
}