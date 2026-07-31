package de.polocloud.node.core

import de.polocloud.common.configuration.ConfigurationHolder
import de.polocloud.node.bootstrap.properties.NodeProperties
import de.polocloud.node.cluster.election.NodeElectionService
import de.polocloud.node.cluster.heartbeat.NodeHeartBeatMonitor
import de.polocloud.node.cluster.heartbeat.NodeHeartBeatService
import de.polocloud.node.cluster.node.NodePruneService
import de.polocloud.node.communication.cli.session.CliSessionManager
import de.polocloud.node.communication.registration.cli.CliRegistrationService
import de.polocloud.node.communication.registration.node.RegistrationManager
import de.polocloud.node.core.configuration.NodeConfigurations
import de.polocloud.node.core.lifecycle.NodeLifecycle
import de.polocloud.node.identity.NodeIdentityService
import de.polocloud.node.identity.provider.FileBasedNodeIdProvider
import de.polocloud.node.identity.provider.NodeIdProvider

class NodeRuntime(
    val launchProperties: NodeProperties,
    holder: ConfigurationHolder<NodeConfigurations>
) {
    val nodeId: NodeIdProvider = FileBasedNodeIdProvider()

    val cliSessionManager = CliSessionManager()
    val cliRegistrationService = CliRegistrationService(
        holder,
        cliSessionManager
    )

    val nodeRegistrationManager = RegistrationManager(
        holder,
        cliRegistrationService,
    )

    val identityService = NodeIdentityService(
        nodeId,
        holder,
        nodeRegistrationManager,
        cliRegistrationService,
        cliSessionManager
    )

    val lifecycle = NodeLifecycle(holder, this)
    val heartBeatService = NodeHeartBeatService()

    val electionService = NodeElectionService()
    val heartBeatMonitor = NodeHeartBeatMonitor(electionService)
    val nodePruneService = NodePruneService()
}