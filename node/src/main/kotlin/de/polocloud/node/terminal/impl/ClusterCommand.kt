package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.Command
import de.polocloud.common.commands.type.KeywordArgument
import de.polocloud.common.configuration.ConfigurationHolder
import de.polocloud.common.system.PolocloudSystemProperties
import de.polocloud.database.DatabaseCredentials
import de.polocloud.node.cluster.heartbeat.NodeHeartBeat
import de.polocloud.node.cluster.heartbeat.NodeHeartBeatRepository
import de.polocloud.node.cluster.node.LocalNodeContainer
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.core.configuration.NodeConfigurations
import de.polocloud.node.group.GroupService
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.terminal.CommandOutput.decimal
import de.polocloud.node.terminal.CommandOutput.timestamp
import de.polocloud.node.terminal.CommandOutput.white
import de.polocloud.node.terminal.WizardPrompt
import de.polocloud.node.terminal.types.NodeArgument
import de.polocloud.proto.NodeState
import de.polocloud.updater.Updater
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
    private val localNodeContainer: LocalNodeContainer,
    private val groupService: GroupService,
    private val serviceProvider: ServiceProvider,
    private val holder: ConfigurationHolder<NodeConfigurations>,
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
        logger.info("  groups: ${white(groupService.findAll().size.toString())}")
        logger.info("  services: ${white(serviceProvider.findAll().size.toString())}")
        logger.info(
            if (totalMemory > 0)
                "  memory: ${white("${usedMemory.roundToInt()}MB / ${totalMemory}MB")} used across online nodes"
            else
                "  memory: ${white("unknown")} (no online node reports its capacity)"
        )
        logger.info("  this node: ${white(localNodeContainer.data.name())}${if (localNodeContainer.data.head) " (head)" else ""}")
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
     * Guides an operator through joining this node into an existing cluster. There is no
     * live "join now, no restart" path in this codebase: `NodeIdentityService.resolve()`
     * — the one place that actually performs a join — only ever runs once, at process
     * boot, before the CLI even exists (see CLUSTER.md §3). So instead of reimplementing
     * that handshake here, this collects what it needs and restarts the process with the
     * join token handed to the next boot via the same system properties
     * (`polocloud.join.token`/`.host`/`.port`) a manual restart would use — the next
     * `resolve()` call does the real work through the already-battle-tested path.
     */
    private fun join() {
        val ownId = localNodeContainer.data.id
        if (NodeRepository.findAll().any { it.id != ownId }) {
            logger.info("This node is already part of a cluster with other nodes — 'cluster join' is only for a fresh, standalone node.")
            return
        }

        if (holder.value.localNode.database is DatabaseCredentials.H2) {
            logger.info("This node's 'localNode.database' (config.json) is still the default embedded H2 database.")
            logger.info("A real cluster requires every node to share the SAME external database (MariaDB/MySQL/PostgreSQL/MongoDB/Redis) — two nodes on separate H2 files are not a cluster.")
            logger.info("Point 'localNode.database' at that shared database, restart this node, then run 'cluster join' again.")
            return
        }

        val answers = ClusterJoinWizard(wizardPrompt).run() ?: return

        logger.info("Restarting to join the cluster at ${answers.host}:${answers.port}...")
        val restarted = Updater.restart(
            listOf(
                "-D${PolocloudSystemProperties.JOIN_TOKEN}=${answers.token}",
                "-D${PolocloudSystemProperties.JOIN_HOST}=${answers.host}",
                "-D${PolocloudSystemProperties.JOIN_PORT}=${answers.port}",
            )
        )
        if (!restarted) {
            logger.warn("Could not determine the running jar to restart automatically. Restart this node manually with:")
            logger.warn("  -D${PolocloudSystemProperties.JOIN_TOKEN}=${answers.token} -D${PolocloudSystemProperties.JOIN_HOST}=${answers.host} -D${PolocloudSystemProperties.JOIN_PORT}=${answers.port}")
        }
    }
}
