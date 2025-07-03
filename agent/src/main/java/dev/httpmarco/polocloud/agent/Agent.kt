package dev.httpmarco.polocloud.agent

import dev.httpmarco.polocloud.agent.detector.DetectorFactoryThread
import dev.httpmarco.polocloud.agent.detector.OnlineStateDetector
import dev.httpmarco.polocloud.agent.grpc.GrpcServerEndpoint
import dev.httpmarco.polocloud.agent.i18n.I18nPolocloudAgent
import dev.httpmarco.polocloud.agent.logging.Logger
import dev.httpmarco.polocloud.agent.runtime.Runtime
import dev.httpmarco.polocloud.platforms.Platform
import dev.httpmarco.polocloud.platforms.PlatformPool
import kotlin.io.path.Path

// global terminal instance for the agent
// this is used to print messages to the console
val logger = Logger()
val i18n = I18nPolocloudAgent();

class Agent {

    val runtime: Runtime
    val platformPool : PlatformPool = PlatformPool.load(Path("local/platforms.json"))

    private val grpcServerEndpoint = GrpcServerEndpoint()

    private val onlineStateDetector = DetectorFactoryThread.bindDetector(OnlineStateDetector())

    companion object {
        val instance = Agent()
    }

    init {
        // display the default log information
        logger.info("Starting PoloCloud Agent...")

        this.runtime = Runtime.create()
        this.grpcServerEndpoint.connect()

        logger.info("Using runtime: ${runtime::class.simpleName}")

        val groups = runtime.groupStorage().items()
        logger.info("Load groups&8 (&7${groups.size}&8): &7" + groups.joinToString(separator = "&8, &7") { it.data.name })
        logger.info("Load platforms&8 (&7${platformPool.platforms.size}&8): &7" + platformPool.platforms.joinToString(separator = "&8, &7") { it.name })

        logger.info("The agent is now &3successfully &7started and ready to use&8!")

        this.onlineStateDetector.detect()
        this.runtime.postInitialize()
    }

    fun close() {
        this.grpcServerEndpoint.close()
        this.onlineStateDetector.close()
    }
}