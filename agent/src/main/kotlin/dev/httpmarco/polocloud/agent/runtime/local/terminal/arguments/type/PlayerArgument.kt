package dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.type

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.InputContext
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.TerminalArgument
import dev.httpmarco.polocloud.shared.player.PolocloudPlayer

class PlayerArgument : TerminalArgument<PolocloudPlayer>("player") {

    override fun buildResult(
        input: String,
        context: InputContext
    ): PolocloudPlayer {
        return Agent.playerStorage.findByName(input)
            ?: throw IllegalArgumentException("Player with name $input not found")
    }

    override fun defaultArgs(context: InputContext): MutableList<String> {
        val args = mutableListOf<String>()

        Agent.playerStorage.findAll().forEach {
            args.add(it.name)
        }

        return args
    }

    override fun predication(rawInput: String): Boolean {
        return Agent.playerStorage.findAll().any {
            it.name.equals(rawInput, ignoreCase = true)
        }
    }
}