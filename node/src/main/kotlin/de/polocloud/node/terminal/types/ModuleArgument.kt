package de.polocloud.node.terminal.types

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.TerminalArgument
import de.polocloud.node.module.ModuleManager

/** Terminal argument that resolves a currently loaded module by its descriptor name. */
class ModuleArgument(key: String, private val moduleManager: ModuleManager) : TerminalArgument<String>(key) {

    override fun defaultArgs(context: InputContext): MutableList<String> =
        moduleManager.list().map { it.descriptor.name }.toMutableList()

    override fun predication(rawInput: String): Boolean =
        rawInput.isNotBlank() && moduleManager.find(rawInput) != null

    override fun buildResult(input: String, context: InputContext): String = input
}
