package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.Command
import de.polocloud.common.configuration.ConfigurationHolder
import de.polocloud.common.os.SystemResources
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.node.cluster.heartbeat.NodeHeartBeatRepository
import de.polocloud.node.cluster.node.LocalNodeContainer
import de.polocloud.node.core.configuration.NodeConfigurations
import de.polocloud.node.group.GroupService
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.terminal.CommandOutput.decimal
import de.polocloud.node.terminal.CommandOutput.timestamp
import de.polocloud.node.terminal.CommandOutput.white
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import java.time.Duration
import kotlin.math.roundToInt

/**
 * Dumps every piece of information the node holds about itself: identity, version,
 * network configuration, live + reported resource usage, process/runtime details and
 * the workload currently placed on this node.
 *
 * Reuses the same section shape as [ClusterCommand.info], but bound to the local node
 * and extended with data that isn't part of the replicated [de.polocloud.node.cluster.node.NodeData]
 * (live JVM/OS stats, full version metadata, resolved network configuration).
 */
class InfoCommand(
    private val localNodeContainer: LocalNodeContainer,
    private val holder: ConfigurationHolder<NodeConfigurations>,
    private val groupService: GroupService,
    private val serviceProvider: ServiceProvider,
) : Command("info", "Show detailed information about this local node") {

    private val logger = LoggerFactory.getLogger(InfoCommand::class.java)

    init {
        defaultExecution { info() }
    }

    private fun info() {
        val node = localNodeContainer.data
        val config = holder.value
        val heartbeat = NodeHeartBeatRepository.find(node.id).maxByOrNull { it.heartBeatAt }
        val runtimeMx = ManagementFactory.getRuntimeMXBean()
        val localServices = serviceProvider.localServices

        logger.info("Node ${node.name()}:")
        logger.info("  id: ${white(node.id.toString())}")
        logger.info("  state: ${white(node.state.toString())}")
        logger.info("  head: ${white(if (node.head) "yes" else "no")}${node.electedAt?.let { " (since ${timestamp(it.toEpochMilliseconds()) { elapsed -> "$elapsed ago" }})" } ?: ""}")
        logger.info("  cluster registration: ${white("${node.hostname}:${node.port}")}")
        logger.info("  first connection: ${timestamp(node.firstConnection.toEpochMilliseconds()) { elapsed -> "$elapsed ago" }}")
        logger.info("  last connection: ${timestamp(node.lastConnection.toEpochMilliseconds()) { elapsed -> "$elapsed ago" }}")

        logger.info("Version:")
        logger.info("  running: ${white(PolocloudVersion.CURRENT.toDisplayString())}")
        logger.info("  registered as: ${white("${node.version} (${node.gitCommitHash})")}")
        logger.info("  channel: ${white(PolocloudVersion.CURRENT.channel.toString())} (debug: ${white(PolocloudVersion.CURRENT.isDebugEnabled.toString())})")
        logger.info("  build: ${white(PolocloudVersion.CURRENT.build.toString())}, commit: ${white(PolocloudVersion.CURRENT.commitId)}")

        logger.info("Network:")
        logger.info("  bind address: ${white("${config.general.bindAddress.hostname}:${config.general.bindAddress.port}")}")
        logger.info("  api address: ${white("${config.general.apiAddress.hostname}:${config.general.apiAddress.port}")}")
        logger.info("  hostname: ${white(config.general.hostname)}")
        logger.info("  advertised service hostname: ${white(config.general.serviceHostname)}")
        logger.info("  cluster registration endpoint: ${white("${config.cluster.registration.hostname}:${config.cluster.registration.port}")}")

        logger.info("Resources:")
        logger.info("  memory reported: ${white(if (node.maxMemory > 0) "${node.maxMemory}MB" else "unknown")}")
        logger.info("  memory live: ${white("${SystemResources.usedMemory().roundToInt()}MB / ${SystemResources.maxMemory().roundToInt()}MB")}")
        logger.info("  cpu live: ${white("${decimal(SystemResources.cpuUsage())}%")}")
        if (heartbeat == null) {
            logger.info("  heartbeat: none received yet")
        } else {
            logger.info("  heartbeat: ${timestamp(heartbeat.heartBeatAt.toEpochMilliseconds()) { elapsed -> "$elapsed ago" }}")
            logger.info("    system:      ${white("${decimal(heartbeat.systemCpuUsage)}% cpu, ${decimal(heartbeat.systemMemoryUsage)}% memory")}")
            logger.info("    application: ${white("${decimal(heartbeat.applicationCpuUsage)}% cpu, ${decimal(heartbeat.applicationMemoryUsage)}% memory")}")
            logger.info("    tps:         ${white(decimal(heartbeat.tps))}")
        }

        logger.info("Process:")
        logger.info("  pid: ${white(ProcessHandle.current().pid().toString())}")
        logger.info("  uptime: ${white(formatDuration(runtimeMx.uptime))}")
        logger.info("  available processors: ${white(Runtime.getRuntime().availableProcessors().toString())}")
        logger.info("  jvm: ${white("${System.getProperty("java.vm.name")} ${System.getProperty("java.version")}")}")
        logger.info("  os: ${white("${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})")}")
        logger.info("  database: ${white(config.localNode.database::class.simpleName ?: "unknown")}")

        logger.info("Workload:")
        logger.info("  groups (cluster-wide): ${white(groupService.findAll().size.toString())}")
        logger.info("  services (cluster-wide): ${white(serviceProvider.findAll().size.toString())}")
        logger.info("  services running here: ${white(localServices.size.toString())}")
        localServices.groupBy { it.groupName }.forEach { (group, services) ->
            logger.info("    $group: ${white(services.size.toString())}")
        }
    }

    private fun formatDuration(millis: Long): String {
        val duration = Duration.ofMillis(millis)
        return "${duration.toHours()}h ${duration.toMinutesPart()}m ${duration.toSecondsPart()}s"
    }
}