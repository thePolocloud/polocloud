package dev.httpmarco.polocloud.sdk.kotlin.services

import com.google.common.util.concurrent.MoreExecutors
import dev.httpmarco.polocloud.shared.service.Service
import dev.httpmarco.polocloud.v1.ServiceControllerGrpc
import dev.httpmarco.polocloud.v1.ServiceFindRequest
import io.grpc.ManagedChannel
import java.util.concurrent.Executor

class ServiceProvider(channel: ManagedChannel?) {

    private val directExecutor: Executor = MoreExecutors.directExecutor()
    private val serviceStub = ServiceControllerGrpc.newBlockingStub(channel)
    private val asyncServiceStub = ServiceControllerGrpc.newFutureStub(channel)

    fun find(): List<Service> {
        return serviceStub.find(ServiceFindRequest.newBuilder().build()).servicesList.map {
            Service(
                it.groupName,
                it.id,
                it.hostname,
                it.port,
                it.state,
                it.serverType,
                it.propertiesMap
            )
        }.toList()
    }

    fun findAsync(): List<Service> {
        return listOf();
    }
}