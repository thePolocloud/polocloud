package de.polocloud.cli.communication.middleware

import io.grpc.Status

/**
 * Thrown by [ClientErrorMiddleware] when a gRPC call fails, regardless of whether the
 * underlying stub raised a [io.grpc.StatusException] (coroutine stubs) or a
 * [io.grpc.StatusRuntimeException] (blocking/async stubs).
 *
 * Callers such as [de.polocloud.cli.terminal.ReadingThread] can catch this specifically
 * to print a concise, user-facing message instead of the raw gRPC stack trace.
 */
class GrpcCallException(
    val grpcStatus: Status,
    cause: Throwable,
) : RuntimeException("gRPC error: $grpcStatus", cause)
