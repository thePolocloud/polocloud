package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.Command
import de.polocloud.common.commands.type.KeywordArgument
import de.polocloud.common.configuration.ConfigurationHolder
import de.polocloud.node.core.configuration.NodeConfigurations
import de.polocloud.node.group.template.GroupTemplateService
import de.polocloud.node.module.ModuleManager
import de.polocloud.node.services.factory.PlatformService
import org.slf4j.LoggerFactory

/**
 * Terminal command that resynchronizes the node's live state with disk/remote sources
 * without requiring a full restart: the `config.json` ([holder]), the downloaded platform
 * template bundle ([PlatformService.resync]), the global template folders
 * ([GroupTemplateService.ensureGlobalTemplates]) and every module under `local/modules/`
 * ([ModuleManager.reload]). Each part can also be reloaded on its own via a subcommand.
 */
class ReloadCommand(
    private val holder: ConfigurationHolder<NodeConfigurations>,
    private val platformService: PlatformService,
    private val moduleManager: ModuleManager,
) : Command("reload", "Resynchronizes config, platforms, templates and modules", "rl") {

    private val logger = LoggerFactory.getLogger(ReloadCommand::class.java)

    init {
        defaultExecution {
            reloadConfig()
            reloadPlatforms()
            reloadTemplates()
            reloadModules()
            logger.info("Reload complete.")
        }

        syntax({
            reloadConfig()
        }, "Reload config.json from disk", KeywordArgument("config"))

        syntax({
            reloadPlatforms()
        }, "Re-download and resync the platform template bundle", KeywordArgument("platforms"))

        syntax({
            reloadTemplates()
        }, "Ensure all global template folders exist", KeywordArgument("templates"))

        syntax({
            reloadModules()
        }, "Unload and reload every module from local/modules", KeywordArgument("modules"))
    }

    private fun reloadConfig() {
        holder.reload()
        logger.info("Config reloaded.")
    }

    private fun reloadPlatforms() {
        platformService.resync()
    }

    private fun reloadTemplates() {
        GroupTemplateService.ensureGlobalTemplates()
        logger.info("Templates reloaded.")
    }

    private fun reloadModules() {
        moduleManager.reload()
        logger.info("Modules reloaded.")
    }
}
