package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.Command
import de.polocloud.common.commands.type.KeywordArgument
import de.polocloud.node.group.GroupService
import de.polocloud.node.services.factory.PlatformService
import de.polocloud.node.services.factory.platform.Platform
import de.polocloud.node.services.factory.platform.PlatformVersionSource
import de.polocloud.node.services.factory.platform.custom.CustomPlatformService
import de.polocloud.node.terminal.CommandOutput.white
import de.polocloud.node.terminal.WizardPrompt
import de.polocloud.node.terminal.types.CustomPlatformArgument
import de.polocloud.node.terminal.types.PlatformArgument
import org.slf4j.LoggerFactory

/**
 * Terminal command for managing platforms. Built-in platforms (loaded from the downloaded
 * `polocloud-platforms` template bundle, see [PlatformService.load]) are read-only here —
 * only custom ones, created via `platform setup`, can be attached new versions or deleted,
 * enforced by [CustomPlatformArgument] only ever matching [Platform.custom] platforms.
 */
class PlatformCommand(
    private val platformService: PlatformService,
    private val customPlatformService: CustomPlatformService,
    private val groupService: GroupService,
    private val wizardPrompt: WizardPrompt,
) : Command("platform", "Manage all platform things here") {

    private val logger = LoggerFactory.getLogger(PlatformCommand::class.java)

    init {
        val platformArgument = PlatformArgument("name", platformService)
        val customPlatformArgument = CustomPlatformArgument("name", platformService)

        syntax({
            val platform = PlatformSetupWizard(customPlatformService, platformService, wizardPrompt).run() ?: return@syntax
            logger.info("Custom platform '${platform.name}' has been created successfully.")
        }, "Interactively create a new custom platform", KeywordArgument("setup"))

        syntax({
            val platform = PlatformVersionSetupWizard(customPlatformService, platformService, wizardPrompt).run() ?: return@syntax
            logger.info("A new version has been attached to '${platform.name}'.")
        }, "Interactively attach a new version to a custom platform", KeywordArgument("version"), KeywordArgument("add"))

        syntax({
            list()
        }, "List all platforms, separated by custom and built-in", KeywordArgument("list"))

        syntax({
            info(it.arg(platformArgument))
        }, "Show detailed information about a platform", KeywordArgument("info"), platformArgument)

        syntax({
            val platform = it.arg(customPlatformArgument)
            val usedBy = groupsUsing(platform.name)
            if (usedBy.isNotEmpty()) {
                logger.info("Platform '${platform.name}' is still used by ${usedBy.joinToString()} — change those groups' platform first.")
                return@syntax
            }
            val custom = customPlatformService.find(platform.name) ?: return@syntax
            customPlatformService.delete(custom)
            logger.info("Custom platform '${platform.name}' has been deleted successfully.")
        }, "Delete a custom platform", KeywordArgument("delete"), customPlatformArgument)
    }

    private fun list() {
        val builtIn = platformService.builtInPlatforms()
        val custom = platformService.customPlatforms()
        if (builtIn.isEmpty() && custom.isEmpty()) {
            logger.info("There are no platforms loaded.")
            return
        }

        logger.info("Built-in platforms (${builtIn.size}):")
        if (builtIn.isEmpty()) {
            logger.info("  (none)")
        } else {
            builtIn.forEach { logger.info("  ${it.name} &8|&r ${it.type}/${it.language} &8|&r versions: ${white(it.versions.size.toString())}") }
        }

        logger.info("Custom platforms (${custom.size}):")
        if (custom.isEmpty()) {
            logger.info("  (none)")
        } else {
            custom.forEach { logger.info("  ${it.name} &8|&r ${it.type}/${it.language} &8|&r versions: ${white(it.versions.size.toString())}") }
        }
    }

    private fun info(platform: Platform) {
        logger.info("Platform ${platform.name} (${if (platform.custom) "custom" else "built-in"}):")
        logger.info("  type: ${white(platform.type.toString())}")
        logger.info("  language: ${white(platform.language.toString())}")
        if (platform.versions.isEmpty()) {
            logger.info("  versions: (none)")
        } else {
            logger.info("  versions:")
            platform.versions.forEach { version ->
                val origin = when (version.source) {
                    PlatformVersionSource.URL -> "url: ${version.downloadUrl}"
                    PlatformVersionSource.LOCAL_FILE -> "local-file: ${version.localFilePath}"
                }
                logger.info("    - ${white(version.version)} ($origin)")
            }
        }
    }

    private fun groupsUsing(platform: String): List<String> =
        groupService.findAll().filter { it.platform.equals(platform, ignoreCase = true) }.map { it.name }
}
