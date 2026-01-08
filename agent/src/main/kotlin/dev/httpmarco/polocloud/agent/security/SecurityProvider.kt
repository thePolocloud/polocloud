package dev.httpmarco.polocloud.agent.security

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.platforms.ServerPlatformForwarding
import dev.httpmarco.polocloud.v1.groups.GroupType
import java.util.UUID

/**
 * Provides security-related information for the agent.
 */
class SecurityProvider {

    /**
     * Short-lived security token used for proxy–agent communication.
     * Consists of 8 random alphanumeric characters.
     */
    val proxySecureToken: String =
        UUID.randomUUID()
            .toString()
            .replace("-", "")
            .take(8)

    /**
     * Determines the globally used forwarding type.
     *
     * If at least one server group uses LEGACY forwarding,
     * LEGACY is selected globally for compatibility reasons.
     * Otherwise, MODERN forwarding is used.
     */
    val globalForwarding: ServerPlatformForwarding
        get() {
            val hasLegacyForwarding = Agent.groupProvider()
                .findAll()
                .filter { it.platform().type == GroupType.SERVER }
                .any { it.platform().forwarding == ServerPlatformForwarding.LEGACY }

            return if (hasLegacyForwarding) {
                ServerPlatformForwarding.LEGACY
            } else {
                ServerPlatformForwarding.MODERN
            }
        }

    fun isLegacyForwarding(): Boolean {
        return globalForwarding == ServerPlatformForwarding.LEGACY
    }

    fun isModernForwarding(): Boolean {
        return globalForwarding == ServerPlatformForwarding.MODERN
    }
}
