package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.type.TextArgument
import de.polocloud.node.services.factory.PlatformService
import de.polocloud.node.services.factory.platform.PlatformVersionSource
import de.polocloud.node.services.factory.platform.custom.CustomPlatform
import de.polocloud.node.services.factory.platform.custom.CustomPlatformService
import de.polocloud.node.services.factory.platform.custom.PlatformSourceValidator
import de.polocloud.node.terminal.WizardPrompt
import de.polocloud.node.terminal.types.CustomPlatformArgument
import de.polocloud.node.terminal.types.PlatformSourceArgument
import de.polocloud.node.terminal.wizard.Wizard
import de.polocloud.node.terminal.wizard.WizardStep

/**
 * The interactive `platform version add` wizard: attaches a new version to an already-existing
 * custom platform, asking the same URL-vs-local-jar question [PlatformSetupWizard] asks for a
 * platform's first version.
 */
class PlatformVersionSetupWizard(
    private val customPlatformService: CustomPlatformService,
    platformService: PlatformService,
    prompt: WizardPrompt,
) : Wizard<CustomPlatform>(prompt, "Platform version setup") {

    private val platformArgument = CustomPlatformArgument("platform", platformService)
    private val versionArgument = TextArgument("version")
    private val sourceArgument = PlatformSourceArgument("source")
    private val locationArgument = TextArgument("location")

    override fun steps(): List<WizardStep<*>> = listOf(
        WizardStep(
            question = { "Which custom platform should this version be attached to?" },
            description = { "Only custom platforms can have versions attached this way — built-in ones are managed by their template." },
            argument = platformArgument,
            label = "Platform",
            format = { it.name },
        ),
        WizardStep(
            question = { "What should this version be called?" },
            description = { "A free-form label, e.g. '1.0.1' — used later to pick this version in 'group create'." },
            argument = versionArgument,
            label = "Version",
            extraValidation = { version, context ->
                val platform = context.arg(platformArgument)
                if (platform.versions.any { it.version == version }) "Platform '${platform.name}' already has a version '$version'." else null
            },
        ),
        WizardStep(
            question = { "Should this version be downloaded from a URL, or copied from a local artifact?" },
            description = { "'url' downloads the artifact on first service start; 'local' copies one already present on this node's filesystem right now." },
            argument = sourceArgument,
            label = "Source",
            format = { it.name.lowercase() },
        ),
        WizardStep(
            question = {
                if (it.arg(sourceArgument) == PlatformVersionSource.URL)
                    "What URL should the artifact be downloaded from?"
                else
                    "What is the local path of the artifact on this node?"
            },
            description = { "Checked before it's accepted: the URL must resolve, or the file must exist (and be a valid jar, for JAVA platforms)." },
            argument = locationArgument,
            label = "Location",
            extraValidation = { location, context ->
                when (context.arg(sourceArgument)) {
                    PlatformVersionSource.URL -> PlatformSourceValidator.verifyUrl(location)
                    PlatformVersionSource.LOCAL_FILE -> PlatformSourceValidator.verifyLocalFile(
                        location,
                        requireJar = context.arg(platformArgument).language == "JAVA",
                    )
                }
            },
        ),
    )

    override fun build(context: InputContext): CustomPlatform {
        return customPlatformService.addVersion(
            context.arg(platformArgument).let { platform ->
                customPlatformService.find(platform.name) ?: error("Custom platform '${platform.name}' was deleted mid-setup.")
            },
            context.arg(versionArgument),
            context.arg(sourceArgument),
            context.arg(locationArgument),
        )
    }
}
