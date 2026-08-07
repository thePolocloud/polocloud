package de.polocloud.node.communication.impl.services

import de.polocloud.common.Address
import de.polocloud.common.communication.server.executor.GrpcServerExecutor
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.communication.grpc.GrpcContextFactory
import de.polocloud.node.communication.grpc.NodeGrpcClient
import de.polocloud.node.communication.handler.CallerAuthorization
import de.polocloud.node.services.LocalServiceLogStreaming
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.cluster.ClusterServiceRouting
import de.polocloud.proto.CopyTemplateRequest
import de.polocloud.proto.CopyTemplateResponse
import de.polocloud.proto.ExecuteServiceCommandRequest
import de.polocloud.proto.ExecuteServiceCommandResponse
import de.polocloud.proto.ServiceApiServiceGrpcKt
import de.polocloud.proto.ServiceCountRequest
import de.polocloud.proto.ServiceCountResponse
import de.polocloud.proto.ServiceListRequest
import de.polocloud.proto.ServiceListResponse
import de.polocloud.proto.ServiceLogLine
import de.polocloud.proto.StopServiceRequest
import de.polocloud.proto.StopServiceResponse
import de.polocloud.proto.StreamServiceLogsRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * gRPC entry point of the API-facing `ServiceApiService`, hosted on the
 * [de.polocloud.node.communication.grpc.ServiceGrpcEndpoint] (and, for peer-to-peer
 * aggregation/forwarding, on [de.polocloud.node.communication.grpc.NodeGrpcEndpoint] too).
 *
 * Every unary RPC delegates to the shared [GrpcServerExecutor] so it runs through the
 * same middleware pipeline as every other request, mirroring
 * [de.polocloud.node.communication.impl.group.GroupApiServiceImpl].
 * `StopService`/`ExecuteServiceCommand`/`CountServices`/`CopyTemplate` share their
 * handlers with the CLI-oriented `ServiceManager` where applicable — those request types
 * are registered once in [de.polocloud.node.communication.grpc.GrpcModule].
 *
 * `StreamServiceLogs` is a server-streaming RPC, so — like
 * [ServiceManagerImpl.streamServiceLogs] — it bypasses [executor] (that pipeline only
 * supports single request/response calls) and is implemented directly here instead.
 */
class ServiceApiServiceImpl(
    private val executor: GrpcServerExecutor,
    private val serviceProvider: ServiceProvider,
) : ServiceApiServiceGrpcKt.ServiceApiServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(ServiceApiServiceImpl::class.java)

    override suspend fun findServices(request: ServiceListRequest): ServiceListResponse {
        return executor.execute(request, GrpcContextFactory.fromGrpc())
    }

    override suspend fun stopService(request: StopServiceRequest): StopServiceResponse {
        return executor.execute(request, GrpcContextFactory.fromGrpc())
    }

    override suspend fun executeServiceCommand(request: ExecuteServiceCommandRequest): ExecuteServiceCommandResponse {
        return executor.execute(request, GrpcContextFactory.fromGrpc())
    }

    override suspend fun countServices(request: ServiceCountRequest): ServiceCountResponse {
        return executor.execute(request, GrpcContextFactory.fromGrpc())
    }

    override suspend fun copyTemplate(request: CopyTemplateRequest): CopyTemplateResponse {
        return executor.execute(request, GrpcContextFactory.fromGrpc())
    }

    override fun streamServiceLogs(request: StreamServiceLogsRequest): Flow<ServiceLogLine> {
        // Not routed through executor (see class doc), so CallerAuthorization is checked
        // directly here rather than at the top of a GrpcServerHandler.handle().
        CallerAuthorization.requireSelfOrTrustedCaller(GrpcContextFactory.fromGrpc(), request.serviceName)

        val local = serviceProvider.findLocal(request.serviceName)
        if (local != null) return LocalServiceLogStreaming.stream(local)

        val service = serviceProvider.find(request.serviceName)
        val node = service?.let { ClusterServiceRouting.resolveOwningNode(it, serviceProvider.nodeId) }
            ?: return emptyFlow()

        return forwardLogStream(node, request)
    }

    /** Relays [node]'s own [streamServiceLogs] response back to this call's collector. */
    private fun forwardLogStream(node: NodeData, request: StreamServiceLogsRequest): Flow<ServiceLogLine> = callbackFlow {
        val client = NodeGrpcClient()
        client.connect(Address(node.hostname, node.port))
        val stub = ServiceApiServiceGrpcKt.ServiceApiServiceCoroutineStub(client.channel())

        val job = launch {
            runCatching {
                stub.streamServiceLogs(request).collect { line -> trySend(line) }
            }.onFailure { ex ->
                logger.warn("Lost forwarded log stream for '{}' from node {}: {}", request.serviceName, node.name(), ex.message)
                close(ex)
            }
        }

        awaitClose {
            job.cancel()
            client.disconnect()
        }
    }
}
