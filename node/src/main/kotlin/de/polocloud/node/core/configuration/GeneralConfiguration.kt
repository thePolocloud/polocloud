package de.polocloud.node.core.configuration

import de.polocloud.common.Address
import de.polocloud.common.GLOBAL_ADDRESS
import de.polocloud.common.utils.localIpAddress
import de.polocloud.common.utils.publicIpAddress
import de.polocloud.node.core.configuration.serializer.LocaleSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class GeneralConfiguration(
    @Serializable(with = LocaleSerializer::class) var locale: Locale = Locale.US,
    var bindAddress: Address = GLOBAL_ADDRESS.withPort(4240),
    var apiAddress: Address = GLOBAL_ADDRESS.withPort(4241),
    var hostname: String = publicIpAddress() ?: localIpAddress(),
    /**
     * Host under which this node's services are advertised to consumers (the proxy
     * bridge / API), i.e. the address a proxy connects a player to.
     *
     * Defaults to loopback so a single-host setup works out of the box. In a multi-host
     * cluster set this to the node's address that is reachable from the *other* nodes
     * (typically the same value as [hostname]) so proxies on other nodes can reach these
     * services. It is deliberately **not** derived from [hostname]: that often defaults to
     * a public IP which is not locally reachable, which would break same-host routing.
     */
    var serviceHostname: String = "127.0.0.1",
    /**
     * Whether the node checks GitHub for a newer PoloCloud release and, if one is
     * found, downloads and applies it before starting up. Runs before services and
     * cluster registration, so a restart doesn't drop live workload. When disabled,
     * the update is only ever applied on demand via the `update` command.
     */
    var autoUpdate: Boolean = false,
)
