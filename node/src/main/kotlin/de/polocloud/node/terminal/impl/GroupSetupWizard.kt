package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.type.BooleanArgument
import de.polocloud.common.commands.type.DoubleArgument
import de.polocloud.common.commands.type.IntArgument
import de.polocloud.common.commands.type.LongArgument
import de.polocloud.common.commands.type.TextArgument
import de.polocloud.node.group.Group
import de.polocloud.node.group.GroupService
import de.polocloud.node.group.PropertyCodec
import de.polocloud.node.services.factory.PlatformService
import de.polocloud.node.terminal.WizardPrompt
import de.polocloud.node.terminal.types.PlatformArgument
import de.polocloud.node.terminal.types.PlatformVersionArgument
import de.polocloud.node.terminal.wizard.Wizard
import de.polocloud.node.terminal.wizard.WizardStep

/**
 * The interactive `group setup` wizard: asks every question `group create` would take as
 * arguments, one at a time, and persists the finished [Group] via [groupService].
 */
class GroupSetupWizard(
    private val groupService: GroupService,
    platformService: PlatformService,
    prompt: WizardPrompt,
) : Wizard<Group>(prompt, "Group setup") {

    private val nameArgument = TextArgument("name")
    private val platformArgument = PlatformArgument("platform", platformService)
    private val versionArgument = PlatformVersionArgument("version", platformService, platformArgument)
    private val memoryArgument = IntArgument("memory", minValue = 1)
    private val startThresholdArgument = DoubleArgument("startThreshold", minValue = 0.0, maxValue = 1.0)
    private val minOnlineArgument = LongArgument("minOnline", minValue = 0L)
    private val maxOnlineArgument = LongArgument("maxOnline", minValue = 0L)
    private val staticArgument = BooleanArgument("static")
    private val fallbackArgument = BooleanArgument("fallback")

    override fun steps(): List<WizardStep<*>> = listOf(
        WizardStep(
            question = { "What should the group be called?" },
            description = { "The name uniquely identifies the group, e.g. for 'group edit' or 'group delete'." },
            argument = nameArgument,
            label = "Name",
            extraValidation = { name, _ ->
                if (groupService.exists(name)) "A group with the name '$name' already exists." else null
            },
        ),
        WizardStep(
            question = { "Which platform should this group run?" },
            description = { "The platform determines which server software services of this group start." },
            argument = platformArgument,
            label = "Platform",
            format = { it.name },
        ),
        WizardStep(
            question = { "Which version of ${it.arg(platformArgument).name} should this group use?" },
            description = { "Only versions available for the chosen platform are accepted." },
            argument = versionArgument,
            label = "Version",
            format = { it.version },
        ),
        WizardStep(
            question = { "How much memory (in MB) should each service of this group get?" },
            description = { "This is the maximum heap size passed to each service process." },
            argument = memoryArgument,
            label = "Memory",
            format = { "$it MB" },
        ),
        WizardStep(
            question = { "At what load ratio should a new service be started?" },
            description = { "A value between 0 and 1, e.g. 0.5 starts a new service once existing ones are half full." },
            argument = startThresholdArgument,
            label = "Start threshold",
        ),
        WizardStep(
            question = { "What is the minimum number of services that should always stay online?" },
            description = { "The queue keeps at least this many services of this group running at all times." },
            argument = minOnlineArgument,
            label = "Min online",
        ),
        WizardStep(
            question = { "What is the maximum number of services allowed online at once?" },
            description = { "The queue never starts more services of this group than this." },
            argument = maxOnlineArgument,
            label = "Max online",
            extraValidation = { maxOnline, context ->
                if (maxOnline < context.arg(minOnlineArgument)) "This must be at least the minimum online count." else null
            },
        ),
        WizardStep(
            question = { "Should this group be static?" },
            description = { "Static groups keep their work directory between restarts instead of starting from a clean template copy each time." },
            argument = staticArgument,
            label = "Static",
            format = { if (it) "yes" else "no" },
        ),
        WizardStep(
            question = { "Should this group be used as a fallback when players have nowhere else to go?" },
            description = { "Fallback groups are preferred as a landing spot when no other group is available." },
            argument = fallbackArgument,
            label = "Fallback",
            format = { if (it) "yes" else "no" },
            // Fallback is a landing spot proxies send players to, so it doesn't apply to proxy groups themselves.
            skip = { it.arg(platformArgument).type.equals("PROXY", ignoreCase = true) },
        ),
    )

    override fun build(context: InputContext): Group {
        val group = groupService.create(
            context.arg(nameArgument),
            context.arg(memoryArgument),
            context.arg(startThresholdArgument),
            context.arg(minOnlineArgument),
            context.arg(maxOnlineArgument),
            context.arg(platformArgument).name,
            context.arg(versionArgument).version,
        )

        val static = context.arg(staticArgument)
        val fallback = context.contains(fallbackArgument) && context.arg(fallbackArgument)
        if (!static && !fallback) {
            return group
        }

        val properties = group.properties.apply { if (fallback) put("fallback", "true") }
        val updated = group.copy(static = static, propertiesJson = PropertyCodec.encode(properties))
        return groupService.update(updated)
    }
}