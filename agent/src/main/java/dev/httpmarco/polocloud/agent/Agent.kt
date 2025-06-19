package dev.httpmarco.polocloud.agent

import dev.httpmarco.polocloud.agent.grpc.GrpcServerEndpoint
import dev.httpmarco.polocloud.agent.i18n.I18nPolocloudAgent
import dev.httpmarco.polocloud.agent.logging.Logger
import dev.httpmarco.polocloud.agent.runtime.Runtime

// global terminal instance for the agent
// this is used to print messages to the console
val logger = Logger()
val i18n = I18nPolocloudAgent();

class Agent {

    lateinit var runtime : Runtime
    private val grpcServerEndpoint = GrpcServerEndpoint()

    companion object {
        val instance = Agent()
    }

    init {
        // display the default log information
        logger.info("Starting PoloCloud Agent...")

        this.runtime = Runtime.create()
        this.grpcServerEndpoint.connect()

        logger.info("Using runtime: ${runtime::class.simpleName}")
        logger.info("Load groups: ${runtime.groupStorage().items().size}")


        this.runtime.postInitialize()

        while (true) {
            // for testing
        }
    }

    fun close() {
        this.grpcServerEndpoint.close()
    }
}