package de.polocloud.node.communication.handler.services

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.services.ServiceProvider
import de.polocloud.proto.ExecuteServiceCommandRequest
import de.polocloud.proto.ExecuteServiceCommandResponse

/**
 * Handles a peer's `ExecuteServiceCommand` request — the cross-node counterpart of the
 * CLI's `service <name> execute`, used when the service is running on a different node
 * than the one the CLI command was issued on. See
 * [de.polocloud.node.terminal.impl.ServiceCommand].
 */
class ExecuteServiceCommandServerHandler(
    private val serviceProvider: ServiceProvider,
) : GrpcServerHandler<ExecuteServiceCommandRequest, ExecuteServiceCommandResponse> {

    override suspend fun handle(request: ExecuteServiceCommandRequest, context: GrpcServerContext): ExecuteServiceCommandResponse {
        val local = serviceProvider.findLocal(request.serviceName)
            ?: return ExecuteServiceCommandResponse.newBuilder()
                .setExecuted(false)
                .setMessage("Service '${request.serviceName}' is not running on this node.")
                .build()

        val executed = local.executeCommand(request.command)
        return ExecuteServiceCommandResponse.newBuilder()
            .setExecuted(executed)
            .setMessage(if (executed) "" else "Process not running.")
            .build()
    }
}