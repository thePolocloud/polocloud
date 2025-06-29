package dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.type

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.runtime.local.terminal.commands.CommandArgument
import dev.httpmarco.polocloud.platforms.Platform

class PlatformArgument(key: String = "platform") : CommandArgument<Platform>(key) {

    override fun buildResult(input: String): Platform {
        return Agent.instance.platformPool.platforms[0]
    }


}