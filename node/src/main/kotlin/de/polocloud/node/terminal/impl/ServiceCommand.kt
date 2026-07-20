package de.polocloud.node.terminal.impl

import de.polocloud.common.Address
import de.polocloud.common.commands.Command
import de.polocloud.common.commands.type.KeywordArgument
import de.polocloud.common.commands.type.StringArrayArgument
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.communication.grpc.NodeGrpcClient
import de.polocloud.node.group.template.GroupTemplateService
import de.polocloud.node.services.LocalService
import de.polocloud.node.services.Service
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.terminal.CliTerminal
import de.polocloud.node.terminal.CommandOutput.decimal
import de.polocloud.node.terminal.CommandOutput.timestamp
import de.polocloud.node.terminal.CommandOutput.white
import de.polocloud.node.terminal.types.ServiceArgument
import de.polocloud.node.terminal.types.TemplateArgument
import de.polocloud.proto.ExecuteServiceCommandRequest
import de.polocloud.proto.GetServiceResourceUsageRequest
import de.polocloud.proto.ServiceManagerGrpcKt
import de.polocloud.proto.StopServiceRequest
import de.polocloud.proto.StreamServiceLogsRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jline.reader.UserInterruptException
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Terminal command for inspecting and controlling the services running on this node.
 *
 * Runs in-process, so it talks to the [ServiceProvider] directly (no gRPC) for a service
 * running here: `list`, `<name>` (info), `<name> shutdown`, `<name> logs` (live tail),
 * `<name> execute <command>` and `<name> copy <templateName>` (re-apply a template onto
 * the running work directory). `shutdown`, `logs` and the CPU/memory usage shown by `<name>`
 * fall back to a [NodeGrpcClient] call to the service's owning node (see [Service.nodeId])
 * when it isn't running here, so all three work cluster-wide regardless of which node's
 * terminal the command is typed in.
 */
class ServiceCommand(
    private val serviceProvider: ServiceProvider,
    private val terminal: CliTerminal,
) : Command("service", "Manage all your cloud services", "ser") {

    private val logger = LoggerFactory.getLogger(ServiceCommand::class.java)

    private val serviceArgument = ServiceArgument("name", serviceProvider)
    private val commandArgument = StringArrayArgument("command")
    private val templateArgument = TemplateArgument("templateName")

    init {
        syntax({
            val services = serviceProvider.findAll()
            if (services.isEmpty()) {
                logger.info("There are no services.")
                return@syntax
            }
            logger.info("Services (${services.size}):")
            services
                .groupBy { resolveNodeLabel(it.nodeId) }
                .toSortedMap()
                .forEach { (nodeLabel, nodeServices) ->
                    logger.info("[3m&8$nodeLabel:&r")
                    nodeServices.forEach { service ->
                        logger.info("  ${service.name()} &8|&r state: ${service.state} &8|&r port: ${service.port}")
                    }
                }
        }, "List all services", KeywordArgument("list"))

        syntax({ context ->
            info(context.arg(serviceArgument))
        }, "Show detailed information about a service", serviceArgument)

        syntax({ context ->
            shutdown(context.arg(serviceArgument))
        }, "Shutdown a service", serviceArgument, KeywordArgument("shutdown"))

        syntax({ context ->
            tailLogs(context.arg(serviceArgument))
        }, "Live-tail the console of a service", serviceArgument, KeywordArgument("logs"))

        syntax({ context ->
            execute(context.arg(serviceArgument), context.arg(commandArgument))
        }, "Execute a command in a service's console", serviceArgument, KeywordArgument("execute"), commandArgument)

        syntax({ context ->
            copy(context.arg(serviceArgument), context.arg(templateArgument))
        }, "Copy a template into a service's work directory", serviceArgument, KeywordArgument("copy"), templateArgument)
    }

    private fun info(service: Service) {
        val local = serviceProvider.findLocal(service.name())
        val usage = resourceUsage(service, local)
        logger.info("Service ${service.name()}:")
        logger.info("  id: ${white(service.id.toString())}")
        logger.info("  group: ${white(service.groupName)}")
        logger.info("  state: ${white(service.state.toString())}")
        logger.info("  node: ${white(resolveNodeLabel(service.nodeId))}")
        logger.info("  host: ${white("${service.hostname}:${service.port}")}")
        logger.info("  pid: ${white((local?.process?.pid() ?: usage?.pid?.takeIf { it >= 0 } ?: "-").toString())}")
        logger.info("  cpu: ${white(usage?.let { "${decimal(it.cpuUsage)}%" } ?: "-")}")
        logger.info("  memory: ${white(usage?.let { "${decimal(it.usedMemory)}MB" } ?: "-")}")
        // Only a co-located LocalService carries a live ping result; a service known only
        // from the DB (e.g. running on another node) has no player count to report here.
        logger.info("  players: ${white(local?.let { "${it.onlinePlayers}/${it.maxPlayers}" } ?: "-")}")
        logger.info("  motd: ${white(local?.motd?.takeIf { it.isNotEmpty() } ?: "-")}")
        logger.info("  static: ${white(local?.let { if (it.static) "yes" else "no" } ?: "-")}")
        logger.info("  work dir: ${white(local?.workDir?.toString() ?: "-")}")
        // startedAt/lastPlayerPollAt only live on a co-located LocalService, same as pid/players above.
        logger.info("  running since: ${local?.startedAt?.takeIf { it > 0 }?.let { timestamp(it) { elapsed -> "running for $elapsed" } } ?: "-"}")
        logger.info("  last communicate: ${local?.lastPlayerPollAt?.takeIf { it > 0 }?.let { timestamp(it) { elapsed -> "$elapsed ago" } } ?: "-"}")
        // Templates actually copied into this instance's work directory on start — not
        // re-read from the group, so it stays accurate even if the group's list changed
        // (or the service isn't running here at all) since this service started.
        logger.info("  templates: ${white(local?.templates?.takeIf { it.isNotEmpty() }?.joinToString() ?: "-")}")
        val properties = local?.properties.orEmpty()
        if (properties.isEmpty()) {
            logger.info("  properties: (none)")
        } else {
            logger.info("  properties:")
            properties.forEach { (key, value) -> logger.info("    - $key=${white(value)}") }
        }
    }

    private data class ResourceUsageView(val cpuUsage: Double, val usedMemory: Double, val pid: Long)

    /**
     * CPU/memory usage for [service], regardless of which node it's running on: sampled
     * directly from [local] if it's co-located, otherwise fetched from the service's owning
     * node over gRPC (mirrors [shutdown]/[execute]). Null if it isn't running anywhere this
     * node can determine, or the owning node couldn't be reached.
     */
    private fun resourceUsage(service: Service, local: LocalService?): ResourceUsageView? {
        if (local != null) {
            return ResourceUsageView(local.cpuUsage, local.usedMemory, local.process?.pid() ?: -1L)
        }

        val node = resolveOwningNode(service) ?: return null
        val client = NodeGrpcClient()
        val result = runCatching {
            client.connect(Address(node.hostname, node.port))
            val stub = ServiceManagerGrpcKt.ServiceManagerCoroutineStub(client.channel())
            val request = GetServiceResourceUsageRequest.newBuilder().setServiceName(service.name()).build()
            val response = runBlocking { stub.getServiceResourceUsage(request) }
            response.takeIf { it.found }?.let { ResourceUsageView(it.cpuUsage, it.usedMemory, it.pid) }
        }.getOrNull()
        client.disconnect()
        return result
    }

    /** Resolves a service's owning `nodeId` to that node's human-readable name (e.g. `node-0`),
     *  falling back to the raw id if the node is unknown (e.g. it left the cluster) or the id
     *  is blank (a service placed before [Service.nodeId] existed). */
    private fun resolveNodeLabel(nodeId: String): String {
        if (nodeId.isBlank()) return "-"
        val node = runCatching { UUID.fromString(nodeId) }.getOrNull()?.let { NodeRepository.find(it) }
        return node?.let { "${it.name()} (${it.hostname})" } ?: nodeId
    }

    /** The [NodeData] a service is running on, or `null` if [Service.nodeId] is blank/unknown. */
    private fun resolveOwningNode(service: Service): NodeData? {
        val id = runCatching { UUID.fromString(service.nodeId) }.getOrNull() ?: return null
        return NodeRepository.find(id)
    }

    private fun shutdown(service: Service) {
        val local = serviceProvider.findLocal(service.name())
        if (local != null) {
            logger.info("Shutting down ${service.name()} ...")
            serviceProvider.shutdownLocal(local)
            logger.info("Service ${service.name()} was stopped.")
            return
        }

        val node = resolveOwningNode(service)
        if (node == null) {
            logger.info("Service ${service.name()} is not running on this node and its owning node is unknown.")
            return
        }

        logger.info("Shutting down ${service.name()} on ${node.name()} ...")
        val client = NodeGrpcClient()
        runCatching {
            client.connect(Address(node.hostname, node.port))
            val stub = ServiceManagerGrpcKt.ServiceManagerCoroutineStub(client.channel())
            val request = StopServiceRequest.newBuilder().setServiceName(service.name()).build()
            val response = runBlocking { stub.stopService(request) }
            if (response.stopped) {
                logger.info("Service ${service.name()} was stopped.")
            } else {
                logger.info("Could not stop ${service.name()}: ${response.message}")
            }
        }.onFailure { ex ->
            logger.info("Could not reach ${node.name()} to stop ${service.name()}: ${ex.message}")
        }
        client.disconnect()
    }

    private fun execute(service: Service, command: String) {
        val local = serviceProvider.findLocal(service.name())
        if (local != null) {
            if (local.executeCommand(command)) {
                logger.info("Executed '$command' in ${service.name()}.")
            } else {
                logger.info("Could not send the command to ${service.name()} (process not running).")
            }
            return
        }

        val node = resolveOwningNode(service)
        if (node == null) {
            logger.info("Service ${service.name()} is not running on this node and its owning node is unknown.")
            return
        }

        val client = NodeGrpcClient()
        runCatching {
            client.connect(Address(node.hostname, node.port))
            val stub = ServiceManagerGrpcKt.ServiceManagerCoroutineStub(client.channel())
            val request = ExecuteServiceCommandRequest.newBuilder().setServiceName(service.name()).setCommand(command).build()
            val response = runBlocking { stub.executeServiceCommand(request) }
            if (response.executed) {
                logger.info("Executed '$command' in ${service.name()} on ${node.name()}.")
            } else {
                logger.info("Could not send the command to ${service.name()} on ${node.name()}: ${response.message}")
            }
        }.onFailure { ex ->
            logger.info("Could not reach ${node.name()} to execute the command on ${service.name()}: ${ex.message}")
        }
        client.disconnect()
    }

    private fun copy(service: Service, templateName: String) {
        val local = serviceProvider.findLocal(service.name())
        if (local == null) {
            logger.info("Service ${service.name()} is not running on this node.")
            return
        }
        val workDir = local.workDir
        if (workDir == null) {
            logger.info("Service ${service.name()} has no work directory yet.")
            return
        }
        GroupTemplateService.copyInto(listOf(templateName), workDir.toFile())
        local.templates = local.templates + templateName
        logger.info("Copied template '$templateName' into ${service.name()}.")
    }

    private fun tailLogs(service: Service) {
        val local = serviceProvider.findLocal(service.name())
        if (local != null) {
            tailLocalLogs(service, local)
            return
        }

        val node = resolveOwningNode(service)
        if (node == null) {
            logger.info("Service ${service.name()} is not running on this node and its owning node is unknown.")
            return
        }

        tailRemoteLogs(service, node)
    }

    private fun tailLocalLogs(service: Service, local: LocalService) {
        logger.info("Tailing logs of ${service.name()} — press Ctrl+C or type 'exit' to stop.")
        // Print the buffered history first, then follow live lines above the input prompt.
        local.recentLogs().forEach { terminal.display(it) }

        val listener: (String) -> Unit = { line -> terminal.display(line) }
        local.addLogListener(listener)
        try {
            awaitExit(service)
        } finally {
            local.removeLogListener(listener)
            logger.info("Stopped tailing ${service.name()}.")
        }
    }

    /**
     * Same as [tailLocalLogs], but the buffered history + live lines come from
     * [ServiceManagerGrpcKt.ServiceManagerCoroutineStub.streamServiceLogs] on the service's
     * owning node instead of a co-located [de.polocloud.node.services.LocalService].
     */
    private fun tailRemoteLogs(service: Service, node: NodeData) {
        logger.info("Tailing logs of ${service.name()} on ${node.name()} — press Ctrl+C or type 'exit' to stop.")

        val client = NodeGrpcClient()
        try {
            client.connect(Address(node.hostname, node.port))
            val stub = ServiceManagerGrpcKt.ServiceManagerCoroutineStub(client.channel())
            val request = StreamServiceLogsRequest.newBuilder().setServiceName(service.name()).build()

            val job = CoroutineScope(Dispatchers.IO).launch {
                stub.streamServiceLogs(request)
                    .onEach { terminal.display(it.line) }
                    .catch { ex -> terminal.display("&cLost log stream from ${node.name()}: ${ex.message}") }
                    .collect()
            }

            try {
                awaitExit(service)
            } finally {
                // Join, not just cancel: waits for the streaming call to actually unwind
                // before the channel below is torn down, avoiding a benign "channel
                // shutting down" error racing the cancellation.
                runBlocking { job.cancelAndJoin() }
                logger.info("Stopped tailing ${service.name()}.")
            }
        } finally {
            client.disconnect()
        }
    }

    /** Blocks on terminal input until the user types `exit`; returns early (without throwing) on Ctrl+C. */
    private fun awaitExit(service: Service) {
        try {
            while (true) {
                val input = terminal.awaitInput("&8[logs:${service.name()}]&r ").trim()
                if (input.equals("exit", ignoreCase = true)) break
            }
        } catch (_: UserInterruptException) {
            // Ctrl+C leaves the tail without terminating the node.
        }
    }
}
