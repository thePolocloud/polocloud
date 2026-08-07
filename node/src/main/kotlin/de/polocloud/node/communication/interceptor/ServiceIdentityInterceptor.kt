package de.polocloud.node.communication.interceptor

import de.polocloud.common.communication.GrpcClientContext
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Grpc
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import java.net.InetSocketAddress
import java.security.cert.X509Certificate

/**
 * gRPC interceptor for [de.polocloud.node.communication.grpc.ServiceGrpcEndpoint].
 *
 * That endpoint is intentionally built with plain `.service(...)` (no interceptors at all —
 * see its class doc), so unlike [de.polocloud.node.communication.grpc.NodeGrpcEndpoint] it never
 * populated [GrpcClientContext.CLIENT_IP] / [CliSessionInterceptor.SUBJECT_CTX_KEY]. That's why
 * [de.polocloud.node.communication.grpc.middleware.LoggingServerMiddleware] always logged
 * `"... from null"` for every request coming from a launched service (e.g. a proxy's or
 * lobby's `ServiceListRequest`) — [de.polocloud.node.communication.grpc.GrpcContextFactory]
 * had nothing to read.
 *
 * Publishes the same two context values [de.polocloud.node.communication.grpc.GrpcContextFactory]
 * already reads for the other endpoint, so no downstream code needs to change: the remote IP,
 * and the certificate CN — which for a service connection is that service's id (e.g. `proxy-1`,
 * `lobby-1`; see [de.polocloud.node.security.ServiceIdentityProvisioner.buildCsr], which signs
 * every service's client certificate with `CN=<serviceId>`).
 *
 * Also publishes that same id under [SERVICE_SUBJECT_CTX_KEY], a key distinct from
 * [CliSessionInterceptor.SUBJECT_CTX_KEY] — this endpoint is the *only* place
 * [SERVICE_SUBJECT_CTX_KEY] is ever set, so [de.polocloud.node.communication.handler.CallerAuthorization]
 * can tell "this call came from a launched service via the SDK" apart from "this call came
 * from a trusted CLI operator or peer node via [de.polocloud.node.communication.grpc.NodeGrpcEndpoint]",
 * which share [CliSessionInterceptor.SUBJECT_CTX_KEY] and would otherwise be indistinguishable.
 */
class ServiceIdentityInterceptor : ServerInterceptor {

    companion object {
        val SERVICE_SUBJECT_CTX_KEY: Context.Key<String> = Context.key("service-identity-subject")
    }

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        var context = Context.current()

        extractIp(call)?.let { context = context.withValue(GrpcClientContext.CLIENT_IP, it) }
        extractServiceId(call)?.let {
            context = context.withValue(CliSessionInterceptor.SUBJECT_CTX_KEY, it)
            context = context.withValue(SERVICE_SUBJECT_CTX_KEY, it)
        }

        return Contexts.interceptCall(context, call, headers, next)
    }

    private fun extractIp(call: ServerCall<*, *>): String? =
        (call.attributes.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR) as? InetSocketAddress)?.address?.hostAddress

    private fun extractServiceId(call: ServerCall<*, *>): String? {
        val cert = call.attributes
            .get(Grpc.TRANSPORT_ATTR_SSL_SESSION)
            ?.peerCertificates
            ?.firstOrNull() as? X509Certificate
            ?: return null

        return X500Name(cert.subjectX500Principal.name)
            .getRDNs(BCStyle.CN)
            .firstOrNull()
            ?.first
            ?.value
            ?.toString()
    }
}
