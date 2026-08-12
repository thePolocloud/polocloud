package de.polocloud.node.terminal.impl

import de.polocloud.common.Address
import de.polocloud.common.commands.Command
import de.polocloud.common.commands.type.KeywordArgument
import de.polocloud.database.DatabaseCredentials
import de.polocloud.node.cluster.heartbeat.NodeHeartBeat
import de.polocloud.node.cluster.heartbeat.NodeHeartBeatRepository
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.communication.registration.node.RegistrationInfo
import de.polocloud.node.core.context.NodeRuntimeContext
import de.polocloud.node.identity.JoinResult
import de.polocloud.node.terminal.CommandOutput.decimal
import de.polocloud.node.terminal.CommandOutput.timestamp
import de.polocloud.node.terminal.CommandOutput.white
import de.polocloud.node.terminal.WizardPrompt
import de.polocloud.node.terminal.types.NodeArgument
import de.polocloud.proto.NodeState
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

/**
 * Terminal command giving an overview of the cluster: known nodes, their live state and
 * resource load, the current head, and cluster-wide group/service totals.
 *
 * `cluster` (no args) prints a summary, `cluster list` lists every node one-line each,
 * and `cluster <name>` shows the detailed view of a single node — the same three-tier
 * shape as [GroupCommand] (list/info) and [ServiceCommand] (list/bare-argument info).
 */
class ClusterCommand(
    private val context: NodeRuntimeContext,
    private val wizardPrompt: WizardPrompt,
) : Command("cluster", "View the state of the cluster and its nodes") {

    private val logger = LoggerFactory.getLogger(ClusterCommand::class.java)

    init {
        val nodeArgument = NodeArgument("name")

        defaultExecution { overview() }

        syntax({
            list()
        }, "List all nodes in the cluster", KeywordArgument("list"))

        syntax({
            info(it.arg(nodeArgument))
        }, "Show detailed information about a node", nodeArgument)

        syntax({
            join()
        }, "Interactively join this fresh, standalone node into an existing cluster", KeywordArgument("join"))
    }

    private fun overview() {
        val nodes = NodeRepository.findAll()
        if (nodes.isEmpty()) {
            logger.info("The cluster has no known nodes.")
            return
        }

        val byState = nodes.groupingBy { it.state }.eachCount()
        val head = nodes.firstOrNull { it.head }

        val onlineNodes = nodes.filter { it.state == NodeState.ONLINE }
        val totalMemory = onlineNodes.sumOf { it.maxMemory }
        val usedMemory = onlineNodes.sumOf { node ->
            latestHeartbeat(node)?.let { (it.systemMemoryUsage / 100.0) * node.maxMemory } ?: 0.0
        }

        logger.info("Cluster overview:")
        logger.info(
            "  nodes: ${white(nodes.size.toString())} total (${byState.entries.joinToString { (state, count) -> "$count ${state.name.lowercase()}" }})"
        )
        logger.info("  head: ${white(head?.name() ?: "(none — election pending)")}")
        logger.info("  groups: ${white(context.groupService.findAll().size.toString())}")
        logger.info("  services: ${white(context.serviceProvider.findAll().size.toString())}")
        logger.info(
            if (totalMemory > 0)
                "  memory: ${white("${usedMemory.roundToInt()}MB / ${totalMemory}MB")} used across online nodes"
            else
                "  memory: ${white("unknown")} (no online node reports its capacity)"
        )
        logger.info("  this node: ${white(context.localNodeContainer.data.name())}${if (context.localNodeContainer.data.head) " (head)" else ""}")
        logger.info("Use 'cluster list' to see all nodes, or 'cluster <name>' for details.")
    }

    private fun list() {
        val nodes = NodeRepository.findAll()
        if (nodes.isEmpty()) {
            logger.info("There are no nodes.")
            return
        }
        logger.info("Nodes (${nodes.size}):")
        nodes.sortedBy { it.nodeIndex }.forEach { node ->
            val load = latestHeartbeat(node)?.let {
                " &8|&r load: ${decimal(it.systemCpuUsage)}% cpu, ${decimal(it.systemMemoryUsage)}% mem"
            } ?: " &8|&r load: -"
            logger.info(
                "  ${node.name()}${if (node.head) " (head)" else ""} &8|&r state: ${node.state} &8|&r host: ${node.hostname}:${node.port}$load"
            )
        }
    }

    private fun info(node: NodeData) {
        val heartbeat = latestHeartbeat(node)
        logger.info("Node ${node.name()}:")
        logger.info("  id: ${white(node.id.toString())}")
        logger.info("  state: ${white(node.state.toString())}")
        logger.info("  head: ${white(if (node.head) "yes" else "no")}${node.electedAt?.let { " (since ${timestamp(it.toEpochMilliseconds()) { elapsed -> "$elapsed ago" }})" } ?: ""}")
        logger.info("  host: ${white("${node.hostname}:${node.port}")}")
        logger.info("  version: ${white("${node.version} (${node.gitCommitHash})")}")
        logger.info("  memory: ${white(if (node.maxMemory > 0) "${node.maxMemory}MB" else "unknown")}")
        logger.info("  first connection: ${timestamp(node.firstConnection.toEpochMilliseconds()) { elapsed -> "$elapsed ago" }}")
        logger.info("  last connection: ${timestamp(node.lastConnection.toEpochMilliseconds()) { elapsed -> "$elapsed ago" }}")
        if (heartbeat == null) {
            logger.info("  heartbeat: none received yet")
        } else {
            logger.info("  heartbeat: ${timestamp(heartbeat.heartBeatAt.toEpochMilliseconds()) { elapsed -> "$elapsed ago" }}")
            logger.info("    system:      ${white("${decimal(heartbeat.systemCpuUsage)}% cpu, ${decimal(heartbeat.systemMemoryUsage)}% memory")}")
            logger.info("    application: ${white("${decimal(heartbeat.applicationCpuUsage)}% cpu, ${decimal(heartbeat.applicationMemoryUsage)}% memory")}")
        }
    }

    private fun latestHeartbeat(node: NodeData): NodeHeartBeat? =
        NodeHeartBeatRepository.find(node.id).maxByOrNull { it.heartBeatAt }

    /**
     * Guides an operator through joining this node into an existing cluster, live and
     * in-process — see [de.polocloud.node.identity.NodeIdentityService.joinLive] for why
     * this no longer needs a process restart the way it used to.
     */
    private fun join() {
        val ownId = context.localNodeContainer.data.id
        if (NodeRepository.findAll().any { it.id != ownId }) {
            logger.info("This node is already part of a cluster with other nodes — 'cluster join' is only for a fresh, standalone node.")
            return
        }

        if (context.holder.value.localNode.database is DatabaseCredentials.H2) {
            logger.info("This node's 'localNode.database' (config.json) is still the default embedded H2 database.")
            logger.info("A real cluster requires every node to share the SAME external database (MariaDB/MySQL/PostgreSQL/MongoDB/Redis) — two nodes on separate H2 files are not a cluster.")
            logger.info("'cluster join' can't switch that live — a node whose own database is empty falls back to becoming its OWN standalone cluster on the next boot, and one that's already shared with other nodes needs a token to boot at all. So:")
            logger.info("  1. Get a fresh registration token from a node already in the target cluster (its standalone CLI's 'cluster connect' command).")
            logger.info("  2. Point 'localNode.database' at the shared database in config.json, then restart this node ONCE with the token already supplied:")
            logger.info("     -Dpolocloud.join.token=<token> -Dpolocloud.join.host=<host> -Dpolocloud.join.port=<port>")
            logger.info("A plain restart without the token will fail to boot once 'localNode.database' points at a database that already has other nodes registered.")
            return
        }

        val answers = ClusterJoinWizard(wizardPrompt).run() ?: return

        logger.info("Joining the cluster at ${answers.host}:${answers.port}...")

        val registrationInfo = RegistrationInfo(answers.token, Address(answers.host, answers.port))
        val group = context.localNodeContainer.data.groupName

        when (val result = context.identityService.joinLive(registrationInfo, group, context)) {
            is JoinResult.Denied ->
                logger.warn("Cluster join denied: ${result.reason}")
            is JoinResult.RecordMissing ->
                logger.warn("Cluster join incomplete: ${result.reason}")
            is JoinResult.Unreachable ->
                logger.warn("Cluster join failed: ${result.reason}")
            is JoinResult.Success -> {
                // adoptJoinedIdentity already updated context.localNodeContainer.data in
                // place, but the visible prompt was only ever rendered once, at terminal
                // construction — refresh it now or the operator keeps seeing the old
                // pre-join name for the rest of this process's life.
                context.cli.refreshPrompt()
                logger.info("Joined the cluster — this node is now '${result.nodeData.name()}'.")
            }
        }
    }
}
