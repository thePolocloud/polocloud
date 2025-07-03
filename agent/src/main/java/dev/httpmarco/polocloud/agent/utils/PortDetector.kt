package dev.httpmarco.polocloud.agent.utils

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.platforms.PlatformType
import java.net.InetSocketAddress
import java.net.ServerSocket

class PortDetector {

    companion object {
        fun nextPort(group: Group): Int {
            var port = if (group.platform().type === PlatformType.PROXY) 25565 else 30000
            while (isPortUsed(port)) {
                port = port + 2
            }
            return port
        }

        private fun isPortUsed(port: Int): Boolean {
            for (service in Agent.instance.runtime.serviceStorage().items()) {
                if (service.port == port) {
                    return true
                }
            }
            try {
                ServerSocket().use { serverSocket ->
                    serverSocket.bind(InetSocketAddress(port))
                    return false
                }
            } catch (_: Exception) {
                return true
            }
        }
    }
}