package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.Command
import de.polocloud.common.commands.type.KeywordArgument
import de.polocloud.moduleapi.ModuleScope
import de.polocloud.node.module.ClusterModuleRegistry
import de.polocloud.node.module.ModuleManager
import de.polocloud.node.terminal.types.ModuleArgument
import org.slf4j.LoggerFactory

/**
 * Terminal command for the modules loaded from `local/modules/` (see [ModuleManager]).
 *
 * `module` (no args) and `module list` both list every loaded module; `module reload`
 * unloads and reloads all of them from disk, `module reload <name>` does just one;
 * `module enable`/`disable <name>` flip a loaded module in place without touching disk;
 * `module cluster` shows the cluster-wide picture built from [ClusterModuleRegistry].
 */
class ModuleCommand(
    private val moduleManager: ModuleManager,
) : Command("module", "Manage modules loaded from local/modules", "mod") {

    private val logger = LoggerFactory.getLogger(ModuleCommand::class.java)

    init {
        val moduleArgument = ModuleArgument("name", moduleManager)

        defaultExecution { list() }

        syntax({
            list()
        }, "List all loaded modules", KeywordArgument("list"))

        syntax({
            moduleManager.reload()
            logger.info("All modules reloaded.")
        }, "Unload and reload every module from local/modules", KeywordArgument("reload"))

        syntax({
            val name = it.arg(moduleArgument)
            moduleManager.reload(name)
            logger.info("Module '$name' reloaded.")
        }, "Unload and reload a single module", KeywordArgument("reload"), moduleArgument)

        syntax({
            val name = it.arg(moduleArgument)
            if (moduleManager.enable(name)) {
                logger.info("Module '$name' enabled.")
            }
        }, "Enable an already-loaded module without reloading it", KeywordArgument("enable"), moduleArgument)

        syntax({
            val name = it.arg(moduleArgument)
            if (moduleManager.disable(name)) {
                logger.info("Module '$name' disabled.")
            }
        }, "Disable an already-loaded module without unloading it", KeywordArgument("disable"), moduleArgument)

        syntax({
            cluster()
        }, "Show which node runs each module across the cluster", KeywordArgument("cluster"))
    }

    private fun list() {
        val modules = moduleManager.list()
        val failures = moduleManager.failures()

        if (modules.isEmpty() && failures.isEmpty()) {
            logger.info("There are no modules loaded.")
            return
        }

        if (modules.isNotEmpty()) {
            logger.info("Modules (${modules.size}):")
            modules.forEach { container ->
                logger.info("  ${container.descriptor.name} v${container.descriptor.version} &8|&r ${container.descriptor.scope} &8|&r ${statusOf(container.enabled, container.descriptor.scope)}")
            }
        }

        if (failures.isNotEmpty()) {
            logger.info("Failed to load (${failures.size}):")
            failures.forEach { (name, reason) ->
                logger.info("  $name &8|&r &cFAILED&r &8- &7$reason")
            }
        }
    }

    private fun cluster() {
        val names = ClusterModuleRegistry.moduleNames()
        if (names.isEmpty()) {
            logger.info("No cluster-wide module status has been reported yet.")
            return
        }
        names.sorted().forEach { name ->
            logger.info("$name:")
            ClusterModuleRegistry.statusesFor(name).sortedBy { it.nodeName }.forEach { status ->
                logger.info("  ${status.nodeName} &8|&r ${statusOf(status.enabled, status.scope)}")
            }
        }
    }

    private fun statusOf(enabled: Boolean, scope: ModuleScope): String = when {
        enabled -> "enabled"
        scope == ModuleScope.SINGLE_ACTIVE -> "standby"
        else -> "disabled"
    }
}
