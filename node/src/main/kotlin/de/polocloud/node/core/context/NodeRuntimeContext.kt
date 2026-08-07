package de.polocloud.node.core.context

import de.polocloud.common.configuration.ConfigurationHolder
import de.polocloud.node.cluster.node.LocalNodeContainer
import de.polocloud.node.communication.grpc.NodeGrpcEndpoint
import de.polocloud.node.communication.grpc.ServiceGrpcEndpoint
import de.polocloud.node.communication.registration.node.RegistrationManager
import de.polocloud.node.core.configuration.NodeConfigurations
import de.polocloud.node.group.GroupService
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.terminal.CliTerminal

class NodeRuntimeContext(
    var holder: ConfigurationHolder<NodeConfigurations>,
    val localNodeContainer: LocalNodeContainer,
    val registrationManager: RegistrationManager,
    val grpcEndpoint: NodeGrpcEndpoint,
    val serviceGrpcEndpoint: ServiceGrpcEndpoint,
    val groupService: GroupService,
    val serviceProvider: ServiceProvider,
) {

    val cli: CliTerminal = CliTerminal(this)

}