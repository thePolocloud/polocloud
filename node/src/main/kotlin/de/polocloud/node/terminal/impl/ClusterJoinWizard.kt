package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.type.BooleanArgument
import de.polocloud.common.commands.type.TextArgument
import de.polocloud.node.terminal.WizardPrompt
import de.polocloud.node.terminal.wizard.Wizard
import de.polocloud.node.terminal.wizard.WizardStep

/** Collected answers of a completed [ClusterJoinWizard] run. */
data class ClusterJoinAnswers(val host: String, val port: Int, val token: String)

/**
 * The interactive `cluster join` wizard: collects the target node's address and a
 * registration token, then confirms before the caller ([ClusterCommand.join]) restarts
 * this process to actually perform the join — see that method for why a restart is
 * necessary rather than joining live.
 */
class ClusterJoinWizard(prompt: WizardPrompt) : Wizard<ClusterJoinAnswers?>(prompt, "Cluster join") {

    private val hostArgument = TextArgument("host")
    private val portArgument = TextArgument("port")
    private val tokenArgument = TextArgument("token")
    private val confirmArgument = BooleanArgument("confirm")

    override fun steps(): List<WizardStep<*>> = listOf(
        WizardStep(
            question = { "What is the hostname/IP of the node you want to join?" },
            description = { "The node whose operator gave you a registration token — reachable on its cluster port." },
            argument = hostArgument,
            label = "Host",
        ),
        WizardStep(
            question = { "What port is that node's cluster endpoint listening on?" },
            description = { "That node's config.json -> cluster.registration port (default 4239)." },
            argument = portArgument,
            label = "Port",
            extraValidation = { value, _ ->
                val port = value.toIntOrNull()
                if (port == null || port !in 1..65535) "Port must be a number between 1 and 65535." else null
            },
        ),
        WizardStep(
            question = { "What registration token did that node's operator give you?" },
            description = { "Minted on the target node (its first-boot log, or the standalone CLI's 'cluster connect' command). Single-use and short-lived." },
            argument = tokenArgument,
            label = "Token",
            format = { "*".repeat(it.length) },
        ),
        WizardStep(
            question = { context -> "Join the cluster at ${context.arg(hostArgument)}:${context.arg(portArgument)} now?" },
            description = { "Restarts this node's process immediately to complete the join — it briefly drops off the cluster while it restarts." },
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
}
