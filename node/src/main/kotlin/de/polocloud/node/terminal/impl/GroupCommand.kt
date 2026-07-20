package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.Command
import de.polocloud.common.commands.type.DoubleArgument
import de.polocloud.common.commands.type.IntArgument
import de.polocloud.common.commands.type.KeywordArgument
import de.polocloud.common.commands.type.LongArgument
import de.polocloud.common.commands.type.StringArrayArgument
import de.polocloud.common.commands.type.TextArgument
import de.polocloud.i18n.api.trInfo
import de.polocloud.node.group.Group
import de.polocloud.node.group.GroupService
import de.polocloud.node.group.PropertyCodec
import de.polocloud.node.group.TemplateCodec
import de.polocloud.node.group.template.GroupTemplateService
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.cluster.ClusterGroupShutdown
import de.polocloud.node.services.factory.PlatformService
import de.polocloud.node.services.queue.GroupNodeEligibility
import de.polocloud.node.terminal.CommandOutput.white
import de.polocloud.node.terminal.WizardPrompt
import de.polocloud.node.terminal.types.GroupArgument
import de.polocloud.node.terminal.types.NodeArgument
import de.polocloud.node.terminal.types.PlatformArgument
import de.polocloud.node.terminal.types.PlatformVersionArgument
import de.polocloud.proto.NodeState
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class GroupCommand(
    val groupService: GroupService,
    platformService: PlatformService,
    private val serviceProvider: ServiceProvider,
    private val wizardPrompt: WizardPrompt,
) : Command("group", "Manage all group things here") {

    private val logger = LoggerFactory.getLogger(GroupCommand::class.java)

    init {
        val groupArgument = GroupArgument("name", groupService)
        val memoryArgument = IntArgument("memory")
        val startThresholdArgument = DoubleArgument("startThreshold")
        val maxOnlineArgument = LongArgument("maxOnline")
        val minOnlineArgument = LongArgument("minOnline")
        val platformArgument = PlatformArgument("platform", platformService)
        val versionArgument = PlatformVersionArgument("version", platformService, platformArgument)

        syntax({
            val group = GroupSetupWizard(groupService, platformService, wizardPrompt).run() ?: return@syntax
            logger.trInfo("node", "node.command.group.created", Pair("name", group.name))
        }, "Interactively create a new group", KeywordArgument("setup"))

        syntax({
            val group = it.arg(groupArgument)
            // Stop the group's running/queued services across every node — not just this
            // one — before removing it, so deleting a group never leaves orphaned
            // processes on peers, nor a stale Service row that would trip the groups
            // table's foreign-key constraint. See ClusterGroupShutdown.
            runBlocking { ClusterGroupShutdown.shutdownAcrossCluster(group.name, serviceProvider) }
            groupService.delete(group)
            logger.trInfo("node", "node.command.group.deleted", Pair("name", group.name))
        }, "Delete a group", KeywordArgument("delete"), groupArgument)

        syntax({
            val groups = groupService.findAll()
            if (groups.isEmpty()) {
                logger.info("There are no groups.")
                return@syntax
            }
            logger.info("Groups (${groups.size}):")
            groups.forEach { group ->
                val fallback = if (group.properties.containsKey("fallback")) " &8|&r fallback: ${white(group.properties["fallback"]!!)}" else ""
                logger.info(
                    "  ${group.name} &8|&r platform: ${group.platform}/${group.version} " +
                        "&8|&r memory: ${group.memory}MB &8|&r online: ${group.minOnline}-${group.maxOnline}$fallback"
                )
            }
        }, "List all groups", KeywordArgument("list"))

        syntax({
            info(it.arg(groupArgument))
        }, "Show detailed information about a group", KeywordArgument("info"), groupArgument)

        // --- edit: change a single group parameter -----------------------------------
        val propertyKeyArgument = TextArgument("key")
        val propertyValueArgument = StringArrayArgument("value")

        syntax({
            val group = it.arg(groupArgument)
            update(group.copy(memory = it.arg(memoryArgument)), "memory", it.arg(memoryArgument))
        }, "Edit a group's memory", KeywordArgument("edit"), groupArgument, KeywordArgument("memory"), memoryArgument)

        syntax({
            val group = it.arg(groupArgument)
            update(group.copy(minOnline = it.arg(minOnlineArgument)), "minOnline", it.arg(minOnlineArgument))
        }, "Edit a group's minimum online count", KeywordArgument("edit"), groupArgument, KeywordArgument("minOnline"), minOnlineArgument)

        syntax({
            val group = it.arg(groupArgument)
            update(group.copy(maxOnline = it.arg(maxOnlineArgument)), "maxOnline", it.arg(maxOnlineArgument))
        }, "Edit a group's maximum online count", KeywordArgument("edit"), groupArgument, KeywordArgument("maxOnline"), maxOnlineArgument)

        syntax({
            val group = it.arg(groupArgument)
            update(group.copy(startThreshold = it.arg(startThresholdArgument)), "startThreshold", it.arg(startThresholdArgument))
        }, "Edit a group's start threshold", KeywordArgument("edit"), groupArgument, KeywordArgument("startThreshold"), startThresholdArgument)

        syntax({
            val group = it.arg(groupArgument)
            val platform = it.arg(platformArgument)
            val version = it.arg(versionArgument)
            update(group.copy(platform = platform.name, version = version.version), "platform", "${platform.name}/${version.version}")
        }, "Edit a group's platform and version", KeywordArgument("edit"), groupArgument, KeywordArgument("platform"), platformArgument, versionArgument)

        syntax({
            val group = it.arg(groupArgument)
            val key = it.arg(propertyKeyArgument)
            val value = it.arg(propertyValueArgument)
            val properties = group.properties.apply { put(key, value) }
            update(group.copy(propertiesJson = PropertyCodec.encode(properties)), "property $key", value)
        }, "Set a group property", KeywordArgument("edit"), groupArgument, KeywordArgument("property"), propertyKeyArgument, propertyValueArgument)

        syntax({
            val group = it.arg(groupArgument)
            val key = it.arg(propertyKeyArgument)
            val properties = group.properties.apply { remove(key) }
            update(group.copy(propertiesJson = PropertyCodec.encode(properties)), "property $key", "(removed)")
        }, "Remove a group property", KeywordArgument("edit"), groupArgument, KeywordArgument("unset"), propertyKeyArgument)

        // --- edit: templates -----------------------------------------------------------
        val templateNameArgument = TextArgument("templateName")

        syntax({
            val group = it.arg(groupArgument)
            val name = it.arg(templateNameArgument)
            if (name in group.templates) {
                logger.info("${group.name} already has template '$name'.")
                return@syntax
            }
            GroupTemplateService.ensure(name)
            val templates = group.templates + name
            update(group.copy(templatesJson = TemplateCodec.encode(templates)), "template", "+$name")
        }, "Add a template to a group", KeywordArgument("edit"), groupArgument, KeywordArgument("template"), KeywordArgument("add"), templateNameArgument)

        syntax({
            val group = it.arg(groupArgument)
            val name = it.arg(templateNameArgument)
            if (name !in group.templates) {
                logger.info("${group.name} does not have template '$name'.")
                return@syntax
            }
            val templates = group.templates - name
            update(group.copy(templatesJson = TemplateCodec.encode(templates)), "template", "-$name")
        }, "Remove a template from a group", KeywordArgument("edit"), groupArgument, KeywordArgument("template"), KeywordArgument("remove"), templateNameArgument)

        val nodeArgument = NodeArgument("node")

        syntax({
            val group = it.arg(groupArgument)
            val node = it.arg(nodeArgument)
            if (node.name() in group.nodes) {
                logger.info("${group.name} is already restricted to node '${node.name()}'.")
                return@syntax
            }
            val nodes = group.nodes + node.name()
            update(group.copy(nodesJson = TemplateCodec.encode(nodes)), "node", "+${node.name()}")
        }, "Restrict a group to an additional node", KeywordArgument("edit"), groupArgument, KeywordArgument("node"), KeywordArgument("add"), nodeArgument)

        syntax({
            val group = it.arg(groupArgument)
            val node = it.arg(nodeArgument)
            if (node.name() !in group.nodes) {
                logger.info("${group.name} is not restricted to node '${node.name()}'.")
                return@syntax
            }
            val nodes = group.nodes - node.name()
            update(group.copy(nodesJson = TemplateCodec.encode(nodes)), "node", "-${node.name()}")
        }, "Remove a node restriction from a group", KeywordArgument("edit"), groupArgument, KeywordArgument("node"), KeywordArgument("remove"), nodeArgument)
    }

    private fun info(group: Group) {
        // Live, in-memory count (mirrors FactoryService.runningCount) rather than a DB
        // read: this only needs "how many are running right now on this node", and the
        // persisted table also holds queued/starting rows the DB alone can't distinguish.
        val running = serviceProvider.localServices.count { it.groupName.equals(group.name, ignoreCase = true) }
        logger.info("Group ${group.name}:")
        logger.info("  platform: ${white("${group.platform}/${group.version}")}")
        logger.info("  memory: ${white("${group.memory}MB")}")
        logger.info("  online: ${white("${group.minOnline}-${group.maxOnline}")} (start threshold: ${white(group.startThreshold.toString())})")
        logger.info("  static: ${white(group.static.toString())}")
        logger.info("  services: ${white("$running running")}")
        logger.info("  nodes: ${white(if (group.nodes.isEmpty()) "all nodes" else group.nodes.joinToString())}")
        // Same NodeRepository-not-ready guard as ServiceQueue's onlineNodes default.
        val onlineNodes = runCatching { NodeRepository.find(NodeState.ONLINE) }.getOrDefault(emptyList())
        val runtimeNodes = GroupNodeEligibility.eligibleOnlineNodes(group, onlineNodes).map { it.name() }
        logger.info("  runtime nodes: ${white(if (runtimeNodes.isEmpty()) "(none online)" else runtimeNodes.joinToString())}")
        logger.info("  templates: ${white(if (group.templates.isEmpty()) "(none)" else group.templates.joinToString())}")
        if (group.properties.isEmpty()) {
            logger.info("  properties: (none)")
        } else {
            logger.info("  properties:")
            group.properties.forEach { (key, value) -> logger.info("    - $key=${white(value)}") }
        }
    }

    private fun update(group: Group, field: String, value: Any) {
        groupService.update(group)
        logger.info("Updated ${group.name}: $field = $value")
    }
}