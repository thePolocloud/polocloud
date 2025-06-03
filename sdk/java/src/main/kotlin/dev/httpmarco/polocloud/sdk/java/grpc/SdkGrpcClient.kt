package dev.httpmarco.polocloud.sdk.java.grpc

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

class SdkGrpcClient {

    var channel: ManagedChannel? = ManagedChannelBuilder.forTarget("localhost").usePlaintext().build()



}