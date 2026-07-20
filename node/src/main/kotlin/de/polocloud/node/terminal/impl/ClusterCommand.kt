package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.Command
import de.polocloud.common.commands.type.KeywordArgument
import de.polocloud.node.cluster.heartbeat.NodeHeartBeat
import de.polocloud.node.cluster.heartbeat.NodeHeartBeatRepository
import de.polocloud.node.cluster.node.LocalNodeContainer
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.group.GroupService
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.terminal.CommandOutput.decimal
import de.polocloud.node.terminal.CommandOutput.timestamp
import de.polocloud.node.terminal.CommandOutput.white
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
    private val localNodeContainer: LocalNodeContainer,
    private val groupService: GroupService,
    private val serviceProvider: ServiceProvider,
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
        nodes.sortedBy { it.index }.forEach { node ->
            val load = latestHeartbeat(node)?.let {
                " &8|&r load: ${decimal(it.systemCpuUsage)}% cpu, ${decimal(it.systemMemoryUsage)}% mem, ${decimal(it.tps)} tps"
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
            logger.info("    tps:         ${white(decimal(heartbeat.tps))}")
        }
    }

    private fun latestHeartbeat(node: NodeData): NodeHeartBeat? =
        NodeHeartBeatRepository.find(node.id).maxByOrNull { it.heartBeatAt }
}
