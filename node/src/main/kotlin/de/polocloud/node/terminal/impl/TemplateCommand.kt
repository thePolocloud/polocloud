package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.Command
import de.polocloud.common.commands.type.KeywordArgument
import de.polocloud.common.commands.type.TextArgument
import de.polocloud.node.group.GroupService
import de.polocloud.node.group.template.GroupTemplateService
import de.polocloud.node.terminal.CommandOutput.white
import de.polocloud.node.terminal.types.TemplateArgument
import org.slf4j.LoggerFactory

/**
 * Terminal command for managing the `local/templates/` folders applied to services on
 * start (see [GroupTemplateService]). Templates aren't a database-backed entity — each
 * one is just a directory referenced by name from [de.polocloud.node.group.Group.templates] —
 * so this command reads/writes the filesystem directly and cross-references
 * [GroupService] only to show/guard which groups use a given template.
 */
class TemplateCommand(
    private val groupService: GroupService,
) : Command("template", "Manage all your templates here", "tpl") {

    private val logger = LoggerFactory.getLogger(TemplateCommand::class.java)

    init {
        val newNameArgument = TextArgument("name")
        val templateArgument = TemplateArgument("name")

        syntax({
            val name = it.arg(newNameArgument)
            if (GroupTemplateService.directoryOf(name).isDirectory) {
                logger.info("A template with the name '$name' already exists.")
                return@syntax
            }
            GroupTemplateService.ensure(name)
            logger.info("Template '$name' has been created successfully.")
        }, "Create a new template", KeywordArgument("create"), newNameArgument)

        syntax({
            val name = it.arg(templateArgument)
            val usedBy = groupsUsing(name)
            if (usedBy.isNotEmpty()) {
                logger.info("Template '$name' is still used by ${usedBy.joinToString()} — remove it from those groups first.")
                return@syntax
            }
            GroupTemplateService.delete(name)
            logger.info("Template '$name' has been deleted successfully.")
        }, "Delete a template", KeywordArgument("delete"), templateArgument)

        syntax({
            list()
        }, "List all templates and the groups using them", KeywordArgument("list"))
    }

    private fun list() {
        val templates = GroupTemplateService.listAll()
        if (templates.isEmpty()) {
            logger.info("There are no templates.")
            return
        }
        logger.info("Templates (${templates.size}):")
        templates.forEach { name ->
            val usedBy = groupsUsing(name)
            logger.info("  $name &8|&r groups: ${white(if (usedBy.isEmpty()) "(none)" else usedBy.joinToString())}")
        }
    }

    private fun groupsUsing(template: String): List<String> =
        groupService.findAll().filter { template in it.templates }.map { it.name }
}