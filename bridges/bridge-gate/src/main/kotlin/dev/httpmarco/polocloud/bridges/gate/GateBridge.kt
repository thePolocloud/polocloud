package dev.httpmarco.polocloud.bridges.gate

import build.buf.gen.minekube.gate.v1.GateServiceClient
import build.buf.gen.minekube.gate.v1.ListServersRequestKt
import build.buf.gen.minekube.gate.v1.Server
import com.connectrpc.ProtocolClientConfig
import com.connectrpc.extensions.GoogleJavaProtobufStrategy
import com.connectrpc.impl.ProtocolClient
import com.connectrpc.okhttp.ConnectOkHttpClient
import com.connectrpc.protocols.NetworkProtocol
import dev.httpmarco.polocloud.bridge.api.BridgeInstance
import okhttp3.OkHttpClient

class GateBridge(hostname: String, port: Int) : BridgeInstance<Server>() {

    val client = ProtocolClient(
        httpClient = ConnectOkHttpClient(OkHttpClient()),
        ProtocolClientConfig(
            host = "http://$hostname:$port",
            serializationStrategy = GoogleJavaProtobufStrategy(),
            networkProtocol = NetworkProtocol.CONNECT,
        ),
    )
    val gateService = GateServiceClient(client)

    override fun generateInfo(
        name: String,
        hostname: String,
        port: Int
    ): Server {
        return TODO()
    }

    override fun registerService(identifier: Server) {
        TODO("Not yet implemented")
    }

    override fun unregisterService(identifier: Server) {
        TODO("Not yet implemented")
    }

    override fun findInfo(name: String): Server? {
        return null
    }
}