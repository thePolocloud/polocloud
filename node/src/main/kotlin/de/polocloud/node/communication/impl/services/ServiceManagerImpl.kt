package de.polocloud.node.communication.impl.services

import de.polocloud.common.communication.server.executor.GrpcServerExecutor
import de.polocloud.node.communication.grpc.GrpcContextFactory
import de.polocloud.node.services.ServiceProvider
import de.polocloud.proto.ExecuteServiceCommandRequest
import de.polocloud.proto.ExecuteServiceCommandResponse
import de.polocloud.proto.ListServicesRequest
import de.polocloud.proto.ListServicesResponse
import de.polocloud.proto.ServiceLogLine
import de.polocloud.proto.ServiceManagerGrpcKt
import de.polocloud.proto.StopGroupServicesRequest
import de.polocloud.proto.StopGroupServicesResponse
import de.polocloud.proto.StopServiceRequest
import de.polocloud.proto.StopServiceResponse
import de.polocloud.proto.StreamServiceLogsRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ServiceManagerImpl(
    private val executor: GrpcServerExecutor,
    private val serviceProvider: ServiceProvider,
) : ServiceManagerGrpcKt.ServiceManagerCoroutineImplBase() {

    override suspend fun listServices(request: ListServicesRequest): ListServicesResponse {
        return executor.execute(request, GrpcContextFactory.fromGrpc())
    }

    override suspend fun stopService(request: StopServiceRequest): StopServiceResponse {
        return executor.execute(request, GrpcContextFactory.fromGrpc())
    }

    override suspend fun executeServiceCommand(request: ExecuteServiceCommandRequest): ExecuteServiceCommandResponse {
        return executor.execute(request, GrpcContextFactory.fromGrpc())
    }

    override suspend fun stopGroupServices(request: StopGroupServicesRequest): StopGroupServicesResponse {
        return executor.execute(request, GrpcContextFactory.fromGrpc())
    }

    /**
     * Streams a locally-running service's buffered recent log lines followed by its live
     * output. Not routed through [executor] like the unary RPCs above — that pipeline only
     * supports a single request/response, whereas this needs to keep pushing lines for as
     * long as the peer stays subscribed (mirrors
     * [de.polocloud.node.communication.impl.node.NodeServiceImpl.listenForEvents]).
     */
    override fun streamServiceLogs(request: StreamServiceLogsRequest): Flow<ServiceLogLine> = callbackFlow {
        val local = serviceProvider.findLocal(request.serviceName)
        if (local == null) {
            close(IllegalStateException("Service '${request.serviceName}' is not running on this node."))
            return@callbackFlow
        }

        local.recentLogs().forEach { line -> trySend(ServiceLogLine.newBuilder().setLine(line).build()) }

        val listener: (String) -> Unit = { line -> trySend(ServiceLogLine.newBuilder().setLine(line).build()) }
        local.addLogListener(listener)

        awaitClose { local.removeLogListener(listener) }
    }
}