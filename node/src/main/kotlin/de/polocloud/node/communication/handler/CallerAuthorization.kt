package de.polocloud.node.communication.handler

import de.polocloud.common.communication.server.context.GrpcServerContext

/**
 * Authorization guard for [de.polocloud.common.communication.server.handler.GrpcServerHandler]s
 * reachable from a launched service via [de.polocloud.node.communication.grpc.ServiceGrpcEndpoint]
 * (the plugin/service SDK).
 *
 * Every service the node launches receives a valid mTLS client certificate off the same
 * cluster CA a CLI operator or peer node uses (see
 * [de.polocloud.node.security.ServiceIdentityProvisioner]), and — because the shared
 * [de.polocloud.node.communication.grpc.GrpcModule] executor/handler registry is used by
 * both [de.polocloud.node.communication.grpc.NodeGrpcEndpoint] (CLI + peer nodes) and
 * [de.polocloud.node.communication.grpc.ServiceGrpcEndpoint] (SDK) — a handler that does
 * nothing extra is reachable equally by a trusted CLI operator and by whatever plugin
 * code happens to be running on a player-facing Minecraft server. Without this guard, a
 * single compromised plugin can delete every group or stop/command any service cluster-wide.
 *
 * [de.polocloud.node.communication.interceptor.ServiceIdentityInterceptor] (only installed
 * on [de.polocloud.node.communication.grpc.ServiceGrpcEndpoint]) publishes the calling
 * service's own id — its certificate CN, e.g. `lobby-1` — into
 * [de.polocloud.node.communication.grpc.GrpcContextFactory]'s `"serviceSubject"` field.
 * That field is `null` for any call arriving via [de.polocloud.node.communication.grpc.NodeGrpcEndpoint]
 * (CLI operator or peer node forwarding an already-authorized request), so these checks
 * are a no-op there.
 */
object CallerAuthorization {

    /**
     * The launched service's own id if [context] is a call from the plugin SDK, or `null`
     * if it's a trusted CLI operator or peer node.
     */
    fun callingServiceId(context: GrpcServerContext): String? = context.get("serviceSubject")

    /**
     * Rejects [context] if it's a service-originated call — cluster/group administration
     * is CLI/peer-only and deliberately not exposed to the plugin SDK at all (no addon in
     * this repo needs to mutate groups; only [de.polocloud.node.group.GroupService.findAll]/`find`
     * are used).
     *
     * @throws IllegalStateException if [context] is a service call (mapped to
     * `UNAUTHENTICATED` by [de.polocloud.node.communication.grpc.middleware.ErrorServerMiddleware]).
     */
    fun requireTrustedCaller(context: GrpcServerContext, action: String) {
        val caller = callingServiceId(context)
        check(caller == null) { "Service '$caller' is not authorized to $action." }
    }

    /**
     * Rejects [context] if it's a service-originated call whose own id isn't
     * [targetServiceName] — a launched service may only act on itself via the SDK
     * (e.g. stopping or commanding itself), never on another service in the cluster.
     * A no-op for a trusted CLI operator or peer node, which may target any service.
     *
     * @throws IllegalStateException if [context] is a service call targeting a different
     * service (mapped to `UNAUTHENTICATED` by [de.polocloud.node.communication.grpc.middleware.ErrorServerMiddleware]).
     */
    fun requireSelfOrTrustedCaller(context: GrpcServerContext, targetServiceName: String) {
        val caller = callingServiceId(context) ?: return
        check(caller.equals(targetServiceName, ignoreCase = true)) {
            "Service '$caller' is not authorized to act on '$targetServiceName'."
        }
    }
}
