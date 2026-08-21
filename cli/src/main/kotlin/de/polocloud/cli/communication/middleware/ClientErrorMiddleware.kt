package de.polocloud.cli.communication.middleware

import de.polocloud.common.communication.client.call.GrpcClientCall
import de.polocloud.common.communication.client.middleware.GrpcClientMiddleware
import io.grpc.StatusException
import io.grpc.StatusRuntimeException

class ClientErrorMiddleware : GrpcClientMiddleware {

    override suspend fun <Response : Any> intercept(
        call: GrpcClientCall<Response>,
        next: suspend () -> Response
    ): Response {
        try {
            return next()
        } catch (exception: StatusException) {
            // Thrown by grpc-kotlin coroutine stubs (e.g. ClusterServiceCoroutineStub) on failure.
            throw GrpcCallException(exception.status, exception)
        } catch (exception: StatusRuntimeException) {
            // Thrown by blocking/async stubs on failure.
            throw GrpcCallException(exception.status, exception)
        }
    }
}