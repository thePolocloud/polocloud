package de.polocloud.node.utils

import de.polocloud.node.services.LocalService
import de.polocloud.node.services.ServiceRepository
import de.polocloud.node.services.factory.FactoryService
import de.polocloud.node.services.factory.platform.Platform
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap

object PortDetector {

    const val SERVER_BASE_PORT = 30000
    const val PROXY_BASE_PORT = 25565

    private val logger = LoggerFactory.getLogger(FactoryService::class.java)

    private val lock = Any()
    private val reservedPorts = ConcurrentHashMap<String, MutableSet<Int>>()

    fun nextPort(service: LocalService, platform: Platform): Int {
        synchronized(lock) {
            var port = if (platform.type.equals("PROXY", ignoreCase = true)) PROXY_BASE_PORT else SERVER_BASE_PORT
            service.properties["startPort"]?.toIntOrNull()?.takeIf { it in 1..65535 }?.let { startPort ->
                port = startPort
                if (isPortUsed(service.nodeId, port)) {
                    logger.warn("The startPort $startPort is already in use, searching for a free one ...")
                }
            }

            while (isPortUsed(service.nodeId, port)) {
                port += 1
            }
            reservedPorts.computeIfAbsent(service.nodeId) { ConcurrentHashMap.newKeySet() }.add(port)
            return port
        }
    }

    /**
     * Releases a port claimed by [nextPort]. Call once the service either becomes
     * visible to [ServiceRepository] (its start succeeded) or its start attempt failed
     * before that point — otherwise the port stays falsely "used" forever.
     */
    fun release(nodeId: String, port: Int) {
        reservedPorts[nodeId]?.remove(port)
    }

    private fun isPortUsed(nodeId: String, port: Int): Boolean {
        if (reservedPorts[nodeId]?.contains(port) == true) {
            return true
        }
        for (service in  ServiceRepository.findAllForNode(nodeId)) {
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