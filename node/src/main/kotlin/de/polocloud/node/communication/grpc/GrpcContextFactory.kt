package de.polocloud.node.communication.grpc

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.GrpcClientContext
import de.polocloud.node.communication.interceptor.CliSessionInterceptor
import de.polocloud.node.communication.interceptor.ServiceIdentityInterceptor

object GrpcContextFactory {

    fun fromGrpc(): GrpcServerContext {
        return GrpcServerContext()
            .with("clientIp", GrpcClientContext.CLIENT_IP.get())
            .with("subject", CliSessionInterceptor.SUBJECT_CTX_KEY.get())
            // Only ever set on ServiceGrpcEndpoint (see ServiceIdentityInterceptor) — null
            // for a CLI operator or peer node, non-null for a launched service. Read by
            // CallerAuthorization to restrict what the plugin SDK is allowed to do.
            .with("serviceSubject", ServiceIdentityInterceptor.SERVICE_SUBJECT_CTX_KEY.get())
    }
}