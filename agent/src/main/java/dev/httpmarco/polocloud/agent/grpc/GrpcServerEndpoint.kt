package dev.httpmarco.polocloud.agent.grpc

import dev.httpmarco.polocloud.agent.logger
import io.grpc.Server
import io.grpc.ServerBuilder

class GrpcServerEndpoint {

    private lateinit var server: Server

    fun connect() {
        this.server = ServerBuilder.forPort(8932).build()
        logger.info("Successfully started gRPC server on port 8932")
    }

    fun close() {
        server.shutdown()
    }
}