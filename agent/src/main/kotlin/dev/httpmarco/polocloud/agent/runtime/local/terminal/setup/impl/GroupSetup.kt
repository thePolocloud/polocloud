package dev.httpmarco.polocloud.agent.runtime.local.terminal.setup.impl

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.groups.GroupData
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.InputContext
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type.*
import dev.httpmarco.polocloud.agent.runtime.local.terminal.setup.Setup
import dev.httpmarco.polocloud.agent.runtime.local.terminal.setup.SetupStep
import dev.httpmarco.polocloud.platforms.PlatformIndex
import dev.httpmarco.polocloud.platforms.PlatformType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class GroupSetup : Setup<Group>("Group setup") {

    private val nameArgument = TextArgument("name")
    private val platformArgument = PlatformArgument("platform")
    private val platformVersionArgument = PlatformVersionArgument(platformArgument)
    private val minMemoryArgument = IntArgument("minMemory", 1)
    private val maxMemoryArgument = IntArgument("maxMemory", 1)
    private val minOnlineServicesArgument = IntArgument("minOnlineServices", 0)
    private val maxOnlineServicesArgument = IntArgument("maxOnlineServices", 0)
    private val fallbackArgument = YesNotArgument("fallback")

    override fun bindQuestion() {
        attach(SetupStep("agent.local-runtime.setup.group.name", nameArgument))
        attach(SetupStep("agent.local-runtime.setup.group.platform", platformArgument) { platform ->
            if (platform.type == PlatformType.SERVER) {
                attach(SetupStep("agent.local-runtime.setup.group.fallback", fallbackArgument))
            }
        })
        attach(SetupStep("agent.local-runtime.setup.group.platform.version", platformVersionArgument))
        attach(SetupStep("agent.local-runtime.setup.group.min-memory", minMemoryArgument))
        attach(SetupStep("agent.local-runtime.setup.group.max-memory", maxMemoryArgument))
        attach(SetupStep("agent.local-runtime.setup.group.min-online-services", minOnlineServicesArgument))
        attach(SetupStep("agent.local-runtime.setup.group.max-online-services", maxOnlineServicesArgument))
    }

    override fun onComplete(result: InputContext): Group {
        val name = result.arg(nameArgument)
        val platform = PlatformIndex(result.arg(platformArgument).name, result.arg(platformVersionArgument).version)
        val minMemory = result.arg(minMemoryArgument)
        val maxMemory = result.arg(maxMemoryArgument)
        val minOnlineServices = result.arg(minOnlineServicesArgument)
        val maxOnlineServices = result.arg(maxOnlineServicesArgument)
        val fallback = if (result.contains(fallbackArgument)) result.arg(fallbackArgument) else null

        val properties = if (fallback != null) {
            buildJsonObject {
                put("fallback", fallback)
            }
        } else {
            emptyMap()
        }

        val group = Group(GroupData(
            name,
            platform,
            minMemory,
            maxMemory,
            minOnlineServices,
            maxOnlineServices,
            emptyList(), // TODO
            properties)
        )

        Agent.runtime.groupStorage().publish(group)
        return group
    }
}