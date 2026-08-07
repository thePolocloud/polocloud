package de.polocloud.node.communication.handler.services

import de.polocloud.common.Address
import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.communication.grpc.NodeGrpcClient
import de.polocloud.node.communication.handler.CallerAuthorization
import de.polocloud.node.group.template.GroupTemplateService
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.cluster.ClusterServiceRouting
import de.polocloud.node.utils.isSafePathSegment
import de.polocloud.proto.CopyTemplateRequest
import de.polocloud.proto.CopyTemplateResponse
import de.polocloud.proto.ServiceApiServiceGrpcKt
import org.slf4j.LoggerFactory

/**
 * Handles a `CopyTemplate` request — the SDK counterpart of the CLI's `service <name>
 * copy <templateName>` (see [de.polocloud.node.terminal.impl.ServiceCommand]), made
 * cluster-wide the same way [StopServiceServerHandler]/[ExecuteServiceCommandServerHandler]
 * are: forwards to the service's owning node if it isn't running locally. Unlike those
 * two, there is no CLI-oriented `ServiceManager` equivalent to forward through, so this
 * forwards to the peer's own `ServiceApiService.CopyTemplate` instead.
 */
class CopyTemplateServerHandler(
    private val serviceProvider: ServiceProvider,
) : GrpcServerHandler<CopyTemplateRequest, CopyTemplateResponse> {

    private val logger = LoggerFactory.getLogger(CopyTemplateServerHandler::class.java)

    override suspend fun handle(request: CopyTemplateRequest, context: GrpcServerContext): CopyTemplateResponse {
        CallerAuthorization.requireSelfOrTrustedCaller(context, request.serviceName)

        if (!isSafePathSegment(request.templateName)) {
            return CopyTemplateResponse.newBuilder()
                .setCopied(false)
                .setMessage("Invalid template name '${request.templateName}'.")
                .build()
        }

        val local = serviceProvider.findLocal(request.serviceName)
        if (local != null) {
            val workDir = local.workDir
                ?: return CopyTemplateResponse.newBuilder()
                    .setCopied(false)
                    .setMessage("Service '${request.serviceName}' has no work directory yet.")
                    .build()

            GroupTemplateService.copyInto(listOf(request.templateName), workDir.toFile())
            local.templates = local.templates + request.templateName
            return CopyTemplateResponse.newBuilder().setCopied(true).build()
        }

        val service = serviceProvider.find(request.serviceName)
        val node = service?.let { ClusterServiceRouting.resolveOwningNode(it, serviceProvider.nodeId) }
            ?: return notRunning(request.serviceName)

        val client = NodeGrpcClient()
        return try {
            client.connect(Address(node.hostname, node.port))
            val stub = ServiceApiServiceGrpcKt.ServiceApiServiceCoroutineStub(client.channel())
            stub.copyTemplate(request)
        } catch (ex: Exception) {
            logger.warn("Failed to forward CopyTemplate for '{}' to node {}: {}", request.serviceName, node.name(), ex.message)
            CopyTemplateResponse.newBuilder()
                .setCopied(false)
                .setMessage("Could not reach the node hosting '${request.serviceName}': ${ex.message}")
                .build()
        } finally {
            client.disconnect()
        }
    }

    private fun notRunning(name: String) = CopyTemplateResponse.newBuilder()
        .setCopied(false)
        .setMessage("Service '$name' is not running.")
        .build()
}
