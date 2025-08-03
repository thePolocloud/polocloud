package dev.httpmarco.polocloud.sdk.kotlin.grpc

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

class SdkGrpcClient {

    var channel: ManagedChannel? = ManagedChannelBuilder.forAddress("127.0.0.1", System.getenv("agent_port").toInt()).usePlaintext().build()


}