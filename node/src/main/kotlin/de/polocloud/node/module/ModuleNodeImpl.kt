package de.polocloud.node.module

import de.polocloud.moduleapi.ModuleNode
import de.polocloud.moduleapi.config.ModuleConfig
import de.polocloud.node.cluster.election.NodeElectionService
import de.polocloud.node.core.context.NodeRuntimeContext
import java.io.File
import java.util.UUID

class ModuleNodeImpl(
    private val context: NodeRuntimeContext,
    private val electionService: NodeElectionService,
    private val moduleDataFolder: File,
) : ModuleNode {

    override val nodeId: UUID get() = context.localNodeContainer.data.id
    override val nodeName: String get() = context.localNodeContainer.data.name()

    override val dataFolder: File
        get() = moduleDataFolder.also { if (!it.exists()) it.mkdirs() }

    override fun isHead(): Boolean = electionService.isHead()

    override fun <T : Any> config(type: Class<T>, default: () -> T): ModuleConfig<T> =
        ModuleConfig(File(dataFolder, "config.yml"), type, default)
}
