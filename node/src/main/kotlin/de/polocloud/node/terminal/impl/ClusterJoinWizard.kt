package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.type.BooleanArgument
import de.polocloud.common.commands.type.TextArgument
import de.polocloud.node.terminal.WizardPrompt
import de.polocloud.node.terminal.wizard.Wizard
import de.polocloud.node.terminal.wizard.WizardStep
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/** Collected answers of a completed [ClusterJoinWizard] run. */
data class ClusterJoinAnswers(val host: String, val port: Int, val token: String)

/**
 * The interactive `cluster join` wizard: collects the target node's address and a
 * registration token, then confirms before the caller ([ClusterCommand.join]) restarts
 * this process to actually perform the join — see that method for why a restart is
 * necessary rather than joining live.
 *
 * The port step probes the address with a plain TCP connect before letting the operator
 * continue — a restart is disruptive (this node briefly drops off the cluster, or exits
 * entirely if [de.polocloud.updater.Updater.restart] can't relaunch it), and the actual
 * gRPC join only runs on the *next* boot, so catching an unreachable target here avoids
 * that whole detour surfacing as a raw stack trace or a silent hang minutes later. It
 * deliberately does not validate the token itself: that would consume it (registration
 * tokens are single-use), so the token is only checked for real by the target during the
 * post-restart join.
 */
class ClusterJoinWizard(prompt: WizardPrompt) : Wizard<ClusterJoinAnswers?>(prompt, "Cluster join") {

    private val hostArgument = TextArgument("host")
    private val portArgument = TextArgument("port")
    private val tokenArgument = TextArgument("token")
    private val confirmArgument = BooleanArgument("confirm")

    override fun steps(): List<WizardStep<*>> = listOf(
        WizardStep(
            question = { "What is the hostname/IP of the node you want to join?" },
            description = { "The node whose operator gave you a registration token — reachable on its cluster.registration port (checked in the next step)." },
            argument = hostArgument,
            label = "Host",
            extraValidation = { value, _ ->
                when {
                    value.isBlank() -> "Host cannot be empty."
                    value.any { it.isWhitespace() } -> "Host must not contain spaces."
                    value.contains("://") -> "Enter just the hostname/IP, not a URL (e.g. '192.168.1.10', not 'http://192.168.1.10')."
                    value.endsWith("/") -> "Enter just the hostname/IP, without a trailing slash."
                    else -> null
                }
            },
        ),
        WizardStep(
            question = { "What port is that node's cluster endpoint listening on?" },
            description = { "That node's config.json -> cluster.registration port (default 4239). If that value was changed, the target node must have been fully restarted afterwards — a config reload alone does not rebind its listener to the new port." },
            argument = portArgument,
            label = "Port",
            extraValidation = { value, context ->
                val port = value.toIntOrNull()
                when {
                    port == null || port !in 1..65535 -> "Port must be a number between 1 and 65535."
                    else -> {
                        val host = context.arg(hostArgument)
                        if (isReachable(host, port)) {
                            null
                        } else {
                            "Could not reach $host:$port within ${CONNECT_TIMEOUT_MILLIS}ms. Check that: the target node is actually running; its cluster.registration port is really $port (changing it requires a full restart of that node, not just a config reload/'reload' command); and no firewall blocks this port between here and there. Type 'back' to fix the host, or try the port again."
                        }
                    }
                }
            },
        ),
        WizardStep(
            question = { "What registration token did that node's operator give you?" },
            description = { "Minted on the target node (its first-boot log, or the standalone CLI's 'cluster connect' command). Single-use and short-lived — request a fresh one if this join is later denied as invalid/expired/reused." },
            argument = tokenArgument,
            label = "Token",
            format = { "*".repeat(it.length) },
            extraValidation = { value, _ ->
                when {
                    value.isBlank() -> "Token cannot be empty."
                    value.any { it.isWhitespace() } -> "Token must not contain spaces — copy it exactly as printed, without surrounding whitespace."
                    else -> null
                }
            },
        ),
        WizardStep(
            question = { context -> "Join the cluster at ${context.arg(hostArgument)}:${context.arg(portArgument)} now?" },
            description = { "Restarts this node's process immediately to complete the join — it briefly drops off the cluster while it restarts. The address was already confirmed reachable, but the token itself (validity, single use, version match) is only checked by the target once this node reboots." },
            argument = confirmArgument,
            label = "Confirm",
            format = { if (it) "yes" else "no" },
        ),
    )

    override fun build(context: InputContext): ClusterJoinAnswers? {
        if (!context.arg(confirmArgument)) return null
        return ClusterJoinAnswers(
            host = context.arg(hostArgument),
            port = context.arg(portArgument).toInt(),
            token = context.arg(tokenArgument),
        )
    }

    private fun isReachable(host: String, port: Int): Boolean =
        try {
            Socket().use { it.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS) }
            true
        } catch (_: IOException) {
            false
        }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 5000
    }
}
