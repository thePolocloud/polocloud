package de.polocloud.api.services

import de.polocloud.proto.CopyTemplateRequest
import de.polocloud.proto.ExecuteServiceCommandRequest
import de.polocloud.proto.ServiceApiServiceGrpcKt
import de.polocloud.proto.ServiceCountRequest
import de.polocloud.proto.ServiceData
import de.polocloud.proto.ServiceListRequest
import de.polocloud.proto.StopServiceRequest
import de.polocloud.proto.StreamServiceLogsRequest
import io.grpc.ManagedChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/**
 * gRPC-backed [ServiceApiClient] that talks to the node's `ServiceApiService`.
 *
 * The channel is obtained lazily through [channelProvider] so the connection is
 * only opened when a call is actually made.
 */
class GrpcServiceApiClient(
    private val channelProvider: () -> ManagedChannel,
) : ServiceApiClient {

    // Every unary call carries a deadline so a misconfigured node surfaces as a clear
    // DEADLINE_EXCEEDED error instead of blocking the caller indefinitely. Not applied to
    // streamServiceLogs — that call is meant to run indefinitely.
    private fun stub() = ServiceApiServiceGrpcKt.ServiceApiServiceCoroutineStub(channelProvider())
        .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)

    override suspend fun findServices(groupFilter: String?, stateFilter: String?): List<ServiceData> {
        val request = ServiceListRequest.newBuilder().apply {
            groupFilter?.let { setGroupFilter(it) }
            stateFilter?.let { setStateFilter(it) }
        }.build()

        return stub().findServices(request).servicesList
    }

    override suspend fun countServices(groupFilter: String?, stateFilter: String?): Int {
        val request = ServiceCountRequest.newBuilder().apply {
            groupFilter?.let { setGroupFilter(it) }
            stateFilter?.let { setStateFilter(it) }
        }.build()

        return stub().countServices(request).count
    }

    override suspend fun stopService(name: String): ServiceCommandResult {
        val request = StopServiceRequest.newBuilder().setServiceName(name).build()
        val response = stub().stopService(request)
        return ServiceCommandResult(response.stopped, response.message)
    }

    override suspend fun executeServiceCommand(name: String, command: String): ServiceCommandResult {
        val request = ExecuteServiceCommandRequest.newBuilder().setServiceName(name).setCommand(command).build()
        val response = stub().executeServiceCommand(request)
        return ServiceCommandResult(response.executed, response.message)
    }

    override suspend fun copyTemplate(name: String, templateName: String): ServiceCommandResult {
        val request = CopyTemplateRequest.newBuilder().setServiceName(name).setTemplateName(templateName).build()
        val response = stub().copyTemplate(request)
        return ServiceCommandResult(response.copied, response.message)
    }

    override fun streamServiceLogs(name: String): Flow<String> {
        val request = StreamServiceLogsRequest.newBuilder().setServiceName(name).build()
        val stub = ServiceApiServiceGrpcKt.ServiceApiServiceCoroutineStub(channelProvider())
        return stub.streamServiceLogs(request).map { it.line }
    }

    private companion object {
        const val DEADLINE_SECONDS = 10L
    }
}
