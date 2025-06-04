package dev.httpmarco.polocloud.agent

import dev.httpmarco.polocloud.agent.grpc.GrpcServerEndpoint
import dev.httpmarco.polocloud.agent.logging.Logger
import dev.httpmarco.polocloud.agent.runtime.Runtime

// global terminal instance for the agent
// this is used to print messages to the console
val logger = Logger()

object Agent {

    private lateinit var runtime : Runtime

    private val grpcServerEndpoint = GrpcServerEndpoint()

    init {
        // display the default log information
        logger.info("Starting PoloCloud Agent...")

        this.runtime = Runtime.create()
        this.grpcServerEndpoint.connect()

        logger.info("Using runtime: ${runtime::class.simpleName}")
        logger.info("Load groups: ${runtime.groupStorage().items().size}")


        while (true) {
            // for testing
        }
    }

    fun close() {
        this.grpcServerEndpoint.close()
    }
}