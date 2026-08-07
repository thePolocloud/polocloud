package de.polocloud.node.communication.handler.services

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.communication.handler.CallerAuthorization
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.cluster.ClusterServiceRouting
import de.polocloud.node.services.cluster.ClusterServiceRouting.forwardToOwningNode
import de.polocloud.proto.ServiceManagerGrpcKt
import de.polocloud.proto.StopServiceRequest
import de.polocloud.proto.StopServiceResponse
import org.slf4j.LoggerFactory

/**
 * Handles a `StopService` request.
 *
 * Stops the service directly if it's running on this node; otherwise looks up its
 * owning node ([de.polocloud.node.services.Service.nodeId]) and forwards the same
 * request there, so a caller never needs to know or care which node a service actually
 * runs on. Used both as the cross-node peer target for the CLI's `service <name>
 * shutdown` (see [de.polocloud.node.terminal.impl.ServiceCommand]) and directly by the
 * API/SDK-facing `ServiceApiService.StopService`, whose callers (plugins) have no
 * cluster topology awareness at all.
 */
class StopServiceServerHandler(
    private val serviceProvider: ServiceProvider,
) : GrpcServerHandler<StopServiceRequest, StopServiceResponse> {

    private val logger = LoggerFactory.getLogger(StopServiceServerHandler::class.java)

    override suspend fun handle(request: StopServiceRequest, context: GrpcServerContext): StopServiceResponse {
        CallerAuthorization.requireSelfOrTrustedCaller(context, request.serviceName)

        val local = serviceProvider.findLocal(request.serviceName)
        if (local != null) {
            serviceProvider.shutdownLocal(local)
            return StopServiceResponse.newBuilder().setStopped(true).build()
        }

        val service = serviceProvider.find(request.serviceName)
        val node = service?.let { ClusterServiceRouting.resolveOwningNode(it, serviceProvider.nodeId) }
            ?: return notRunning(request.serviceName)

        return forwardToOwningNode(
            node = node,
            logger = logger,
            requestName = "StopService",
            serviceName = request.serviceName,
            onFailure = { message ->
                StopServiceResponse.newBuilder().setStopped(false).setMessage(message).build()
            },
        ) { client ->
            val stub = ServiceManagerGrpcKt.ServiceManagerCoroutineStub(client.channel())
            stub.stopService(request)
        }
    }

    private fun notRunning(name: String) = StopServiceResponse.newBuilder()
        .setStopped(false)
        .setMessage("Service '$name' is not running.")
        .build()
}
