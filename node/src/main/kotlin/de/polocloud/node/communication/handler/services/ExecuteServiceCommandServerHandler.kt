package de.polocloud.node.communication.handler.services

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.communication.handler.CallerAuthorization
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.cluster.ClusterServiceRouting
import de.polocloud.node.services.cluster.ClusterServiceRouting.forwardToOwningNode
import de.polocloud.proto.ExecuteServiceCommandRequest
import de.polocloud.proto.ExecuteServiceCommandResponse
import de.polocloud.proto.ServiceManagerGrpcKt
import org.slf4j.LoggerFactory

/**
 * Handles an `ExecuteServiceCommand` request.
 *
 * Runs the command directly if the service is running on this node; otherwise looks up
 * its owning node ([de.polocloud.node.services.Service.nodeId]) and forwards the same
 * request there. Used both as the cross-node peer target for the CLI's `service <name>
 * execute` (see [de.polocloud.node.terminal.impl.ServiceCommand]) and directly by the
 * API/SDK-facing `ServiceApiService.ExecuteServiceCommand`, whose callers (plugins) have
 * no cluster topology awareness at all.
 */
class ExecuteServiceCommandServerHandler(
    private val serviceProvider: ServiceProvider,
) : GrpcServerHandler<ExecuteServiceCommandRequest, ExecuteServiceCommandResponse> {

    private val logger = LoggerFactory.getLogger(ExecuteServiceCommandServerHandler::class.java)

    override suspend fun handle(request: ExecuteServiceCommandRequest, context: GrpcServerContext): ExecuteServiceCommandResponse {
        CallerAuthorization.requireSelfOrTrustedCaller(context, request.serviceName)

        val local = serviceProvider.findLocal(request.serviceName)
        if (local != null) {
            val executed = local.executeCommand(request.command)
            return ExecuteServiceCommandResponse.newBuilder()
                .setExecuted(executed)
                .setMessage(if (executed) "" else "Process not running.")
                .build()
        }

        val service = serviceProvider.find(request.serviceName)
        val node = service?.let { ClusterServiceRouting.resolveOwningNode(it, serviceProvider.nodeId) }
            ?: return notRunning(request.serviceName)

        return forwardToOwningNode(
            node = node,
            logger = logger,
            requestName = "ExecuteServiceCommand",
            serviceName = request.serviceName,
            onFailure = { message ->
                ExecuteServiceCommandResponse.newBuilder().setExecuted(false).setMessage(message).build()
            },
        ) { client ->
            val stub = ServiceManagerGrpcKt.ServiceManagerCoroutineStub(client.channel())
            stub.executeServiceCommand(request)
        }
    }

    private fun notRunning(name: String) = ExecuteServiceCommandResponse.newBuilder()
        .setExecuted(false)
        .setMessage("Service '$name' is not running.")
        .build()
}
