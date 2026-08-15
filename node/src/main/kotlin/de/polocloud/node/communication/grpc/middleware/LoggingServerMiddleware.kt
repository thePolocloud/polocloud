package de.polocloud.node.communication.grpc.middleware

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.middleware.GrpcServerMiddleware
import de.polocloud.node.cluster.node.NodeRepository
import org.slf4j.LoggerFactory
import java.util.UUID

class LoggingServerMiddleware : GrpcServerMiddleware {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun <Request : Any, Response : Any> intercept(
        request: Request,
        context: GrpcServerContext,
        next: suspend () -> Response
    ): Response {
        // "subject" is the certificate CN — a service id (e.g. "proxy-1"), a CLI username, or
        // (for node-to-node traffic, e.g. a peer fetching this node's ServiceListRequest view)
        // a node's UUID — and is far more useful here than the raw IP, so prefer it when
        // present. A node UUID on its own is meaningless in a log line, so resolve it to that
        // node's readable name (e.g. "node-1") the same way ServiceCommand.resolveNodeLabel does.
        val source = context.get<String>("subject")?.let(::resolveSource) ?: context.get<String>("clientIp")

        logger.debug("[->] ${request::class.simpleName} from $source")

        val result = next()

        logger.debug("[<-] ${result::class.simpleName}")

        return result
    }

    private fun resolveSource(subject: String): String {
        val uuid = runCatching { UUID.fromString(subject) }.getOrNull() ?: return subject
        return NodeRepository.find(uuid)?.name() ?: subject
    }
}