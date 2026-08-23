package de.polocloud.modules.status

import de.polocloud.modules.status.http.StatusHttpServer
import de.polocloud.moduleapi.PolocloudModule
import de.polocloud.moduleapi.config.ModuleConfig
import de.polocloud.moduleapi.config.config

/**
 * Exposes this node's view of the cluster (which of the [StatusConfig.groups] are
 * online, player counts, …) as a REST API on [StatusConfig.port], optionally hosting a
 * static status website bundled into this module's own jar alongside it. See
 * [StatusConfig] for what's configurable.
 *
 * [de.polocloud.moduleapi.ModuleScope.EVERY_NODE] (the default): each node that loads
 * this module runs its own independent HTTP server, but every one of them answers with
 * the same cluster-wide service data — [de.polocloud.api.services.ServiceService]
 * already aggregates every online node's services regardless of which node is asked.
 */
class StatusModule : PolocloudModule() {

    private lateinit var config: ModuleConfig<StatusConfig>
    private var httpServer: StatusHttpServer? = null

    override fun onLoad() {
        config = node.config { StatusConfig() }
    }

    override fun onEnable() {
        val server = StatusHttpServer(config.value, StatusService(node, config), logger)
        server.start()
        httpServer = server
    }

    override fun onDisable() {
        httpServer?.close()
        httpServer = null
    }
}
