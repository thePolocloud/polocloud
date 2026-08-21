package de.polocloud.node.communication.interceptor

import de.polocloud.common.communication.GrpcClientContext
import de.polocloud.i18n.api.TranslationService
import de.polocloud.i18n.api.trWarn
import de.polocloud.node.core.environment.NodeEnvironment
import io.grpc.*
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

/**
 * gRPC interceptor that validates incoming CLI connections against an IP whitelist.
 *
 * Rejects connections from non-whitelisted IPs with [Status.PERMISSION_DENIED].
 * Wildcard patterns (e.g. `127.0.0.*`) are supported.
 *
 * @param config CLI access configuration containing the allowed IP list
 */
class IpWhitelistInterceptor : ServerInterceptor {

    private val logger = LoggerFactory.getLogger(IpWhitelistInterceptor::class.java)

    override fun <T, R> interceptCall(
        call: ServerCall<T, R>,
        headers: Metadata,
        next: ServerCallHandler<T, R>,
    ): ServerCall.Listener<T> {
        if (!NodeEnvironment.configurations.cluster.cliAccess.enabled) {
            val remote = call.attributes.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR).toString()
            logger.trWarn("cluster", "cli.access.disabled.log", "client" to remote)
            return deny(call, "cli.access.disabled")
        }

        val remote = call.attributes.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR)

        if (remote !is InetSocketAddress) {
            logger.trWarn("cluster", "cluster.cli.ip.unknown")
            return deny(call, "cluster.cli.ip.unknown")
        }

        val ip = remote.address.hostAddress

        if (!isAllowed(ip)) {
            logger.trWarn("cluster", "cluster.cli.ip.notAllowed.log", "ip" to ip)
            return deny(call, "cluster.cli.ip.notAllowed")
        }

        logger.debug("CLI connection allowed from IP: $ip")
        val context = Context.current().withValue(GrpcClientContext.CLIENT_IP, ip)
        return Contexts.interceptCall(context, call, headers, next)
    }

    private fun isAllowed(ip: String): Boolean =
        NodeEnvironment.configurations.cluster.cliAccess.allowedIps.any { wildcardToRegex(it).matches(ip) }

    /**
     * Converts a wildcard pattern to a regex.
     *
     * Examples:
     * - `127.0.0.*` → `^127\.0\.0\..*$`
     * - `*`         → `^.*$`
     */
    private fun wildcardToRegex(pattern: String): Regex {
        val escaped = pattern.split("*").joinToString(".*") { Regex.escape(it) }
        return Regex("^$escaped$")
    }

    private fun <T, R> deny(
        call: ServerCall<T, R>,
        translationKey: String,
    ): ServerCall.Listener<T> {
        call.close(
            Status.PERMISSION_DENIED.withDescription(
                TranslationService.tr("cluster", translationKey)
            ),
            Metadata(),
        )
        return object : ServerCall.Listener<T>() {}
    }
}
