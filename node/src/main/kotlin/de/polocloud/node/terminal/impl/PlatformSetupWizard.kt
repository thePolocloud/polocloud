package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.type.TextArgument
import de.polocloud.node.services.factory.PlatformService
import de.polocloud.node.services.factory.platform.custom.CustomPlatform
import de.polocloud.node.services.factory.platform.custom.CustomPlatformService
import de.polocloud.node.services.factory.platform.PlatformVersionSource
import de.polocloud.node.services.factory.platform.custom.PlatformSourceValidator
import de.polocloud.service.factory.process.PlatformRuntime
import de.polocloud.node.terminal.WizardPrompt
import de.polocloud.node.terminal.types.PlatformSourceArgument
import de.polocloud.node.terminal.types.PlatformTypeArgument
import de.polocloud.node.terminal.wizard.Wizard
import de.polocloud.node.terminal.wizard.WizardStep

/**
 * The interactive `platform setup` wizard: creates a brand-new custom platform together with
 * its first version, asking the same two-source (URL vs. node-local jar) question
 * [PlatformVersionSetupWizard] asks when attaching further versions later.
 */
class PlatformSetupWizard(
    private val customPlatformService: CustomPlatformService,
    private val platformService: PlatformService,
    prompt: WizardPrompt,
) : Wizard<CustomPlatform>(prompt, "Platform setup") {

    private val nameArgument = TextArgument("name")
    private val typeArgument = PlatformTypeArgument("type")
    private val languageArgument = TextArgument("language")
    private val versionArgument = TextArgument("version")
    private val sourceArgument = PlatformSourceArgument("source")
    private val locationArgument = TextArgument("location")

    override fun steps(): List<WizardStep<*>> = listOf(
        WizardStep(
            question = { "What should the platform be called?" },
            description = { "The name uniquely identifies the platform, e.g. for 'group create' or 'platform version add'." },
            argument = nameArgument,
            label = "Name",
            extraValidation = { name, _ ->
                if (platformService.find(name) != null) "A platform with the name '$name' already exists (built-in or custom)." else null
            },
        ),
        WizardStep(
            question = { "Is this platform a SERVER or a PROXY?" },
            description = { "Proxies get the Polocloud bridge plugin installed automatically; servers don't." },
            argument = typeArgument,
            label = "Type",
        ),
        WizardStep(
            question = { "Which runtime language does this platform use?" },
            description = { "Determines how the artifact is launched. 'JAVA' and 'GO' are supported out of the box." },
            argument = languageArgument,
            label = "Language",
            format = { it.uppercase() },
            extraValidation = { language, _ ->
                runCatching { PlatformRuntime.forLanguage(language.uppercase()) }
                    .fold({ null }, { "No runtime is registered for language '${language.uppercase()}'." })
            },
        ),
        WizardStep(
            question = { "What should this first version be called?" },
            description = { "A free-form label, e.g. '1.0.0' — used later to pick this version in 'group create'." },
            argument = versionArgument,
            label = "Version",
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
                        requireJar = context.arg(languageArgument).uppercase() == "JAVA",
                    )
                }
            },
        ),
    )

    override fun build(context: InputContext): CustomPlatform {
        val platform = customPlatformService.create(
            context.arg(nameArgument),
            context.arg(typeArgument),
            context.arg(languageArgument),
        )
        return customPlatformService.addVersion(
            platform,
            context.arg(versionArgument),
            context.arg(sourceArgument),
            context.arg(locationArgument),
        )
    }
}
