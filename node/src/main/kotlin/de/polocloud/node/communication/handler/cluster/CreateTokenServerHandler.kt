package de.polocloud.node.communication.handler.cluster

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.communication.registration.node.token.RegistrationToken
import de.polocloud.node.communication.registration.node.token.toProto
import de.polocloud.node.core.environment.NodeEnvironment
import de.polocloud.proto.CreateTokenRequest
import de.polocloud.proto.CreateTokenResponse
import java.util.UUID

class CreateTokenServerHandler(
    // Injectable so the authorization check can be unit-tested without a fully booted
    // NodeEnvironment — see CreateTokenServerHandlerTest.
    private val createToken: () -> RegistrationToken = {
        NodeEnvironment.runtime.nodeRegistrationManager.tokenManger.create()
    },
) : GrpcServerHandler<CreateTokenRequest, CreateTokenResponse> {

    override suspend fun handle(
        request: CreateTokenRequest,
        context: GrpcServerContext
    ): CreateTokenResponse {
        // This RPC is meant for CLI operators onboarding a new node, not for peer nodes
        // themselves — the cluster gRPC port authenticates both against the same CA (see
        // NodeGrpcEndpoint), so without this check any node reachable on that port could
        // mint a fresh registration token for itself or a third party, bypassing whatever
        // control an operator has over who's allowed to join. A node's certificate CN is
        // always its own id (see RegistrationService.registerNode), so a subject that
        // resolves to a known node id can only be a peer node, never a CLI session.
        val subject = context.get<String>("subject")
        val callerIsNode = subject
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?.let { NodeRepository.find(it) != null }
            ?: false

        if (callerIsNode) {
            throw IllegalStateException("Only CLI operators may create registration tokens")
        }

        return CreateTokenResponse.newBuilder()
            .setToken(createToken().toProto())
            .build()
    }
}