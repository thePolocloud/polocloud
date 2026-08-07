package de.polocloud.node.communication.grpc.middleware

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.middleware.GrpcServerMiddleware
import io.grpc.Status
import org.slf4j.LoggerFactory

class ErrorServerMiddleware : GrpcServerMiddleware {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun <Request : Any, Response : Any> intercept(
        request: Request,
        context: GrpcServerContext,
        next: suspend () -> Response
    ): Response {
        try {
            return next()
        } catch (ex: IllegalStateException) {
            throw Status.UNAUTHENTICATED
                .withDescription(ex.message)
                .asRuntimeException()
        } catch (ex: Exception) {
            // Log the real error (with stack trace) server-side before collapsing it into a
            // generic INTERNAL status for the client — otherwise the actual cause is only ever
            // visible to the caller as a bare message, never in this node's own logs.
            logger.error("Unhandled error while handling {}", request::class.simpleName, ex)
            throw Status.INTERNAL
                .withDescription(ex.message)
                .asRuntimeException()
        }
    }
}