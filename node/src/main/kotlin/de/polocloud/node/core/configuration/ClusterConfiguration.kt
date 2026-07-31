package de.polocloud.node.core.configuration

import de.polocloud.common.Address
import de.polocloud.common.GLOBAL_ADDRESS
import de.polocloud.node.core.configuration.cluster.CliAccessConfiguration
import de.polocloud.node.core.configuration.cluster.ClusterTimingConfiguration
import kotlinx.serialization.Serializable

@Serializable
data class ClusterConfiguration(
    var registration: Address = GLOBAL_ADDRESS.withPort(4239),
    var cliAccess: CliAccessConfiguration = CliAccessConfiguration(),
    var timing: ClusterTimingConfiguration = ClusterTimingConfiguration(),
)
