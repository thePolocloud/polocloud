package dev.httpmarco.polocloud.sdk.java

import dev.httpmarco.polocloud.sdk.java.groups.GroupProvider
import dev.httpmarco.polocloud.sdk.java.grpc.SdkGrpcClient
import dev.httpmarco.polocloud.sdk.java.services.ServiceProvider

class Polocloud {

    private val grpcClient = SdkGrpcClient()
    private val serviceProvider = ServiceProvider()
    private val groupProvider = GroupProvider()

    companion object {
        val instance = Polocloud()
    }

    fun serviceProvider() = serviceProvider

    fun groupProvider() = groupProvider

    fun available(): Boolean {
        return !grpcClient.channel?.isShutdown!! && !grpcClient.channel?.isTerminated!!
    }
}