package dev.httpmarco.polocloud.sdk.kotlin

import dev.httpmarco.polocloud.sdk.kotlin.events.EventProvider
import dev.httpmarco.polocloud.sdk.kotlin.groups.GroupProvider
import dev.httpmarco.polocloud.sdk.kotlin.grpc.SdkGrpcClient
import dev.httpmarco.polocloud.sdk.kotlin.services.ServiceProvider
import dev.httpmarco.polocloud.shared.PolocloudShared

class Polocloud : PolocloudShared() {

    private val grpcClient = SdkGrpcClient()
    private val serviceProvider = ServiceProvider(grpcClient.channel)
    private val groupProvider = GroupProvider(grpcClient.channel)
    private val eventProvider = EventProvider(grpcClient.channel)

    companion object {
        private val instance = Polocloud()

        fun instance(): Polocloud {
            return instance
        }
    }

    fun serviceProvider() = serviceProvider

    fun groupProvider() = groupProvider

    fun available(): Boolean {
        return !grpcClient.channel?.isShutdown!! && !grpcClient.channel?.isTerminated!!
    }

    fun selfServiceName(): String {
        return System.getenv("service-name")
    }

    override fun eventProvider() = eventProvider

}